package com.github.gpaddons.gpextraprotections;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ProtectNametaggedEntities implements Listener {

	private final Set<EntityType> typeBlacklist = EnumSet.noneOf(EntityType.class);
	private final Set<NamespacedKey> keyBlacklist = new HashSet<>();
	private final Set<String> metaBlacklist = new HashSet<>();
	private final Set<Pattern> nameBlacklist = new HashSet<>();

	public ProtectNametaggedEntities(GPExtraProtections plugin) {
		loadConfig(plugin);
	}

	private void loadConfig(GPExtraProtections plugin) {
		// Load blacklisted entity types.
		loadBlacklist(plugin, typeBlacklist, "type_blacklist", EntityType::valueOf);

		// Load blacklisted PersistentDataContainer keys.
		loadBlacklist(plugin, keyBlacklist, "persistent_data_blacklist", key -> {
			String[] split = key.split(":");
			// It's super possible to do extra parsing and mandate that the namespace of the key must exist,
			// but that's a lot of additional completely unnecessary roundabout work when we really just want to
			// let users check whatever conditions, not just conditions that Spigot approves of existing.
			// Because we're nice, use deprecated constructor to support whatever user desires.
			// noinspection deprecation
			return new NamespacedKey(split[0], split[1]);
		});

		loadBlacklist(plugin, metaBlacklist, "metadata_blacklist", Function.identity());

		loadBlacklist(plugin, nameBlacklist, "name_pattern_blacklist", key -> Pattern.compile(key, Pattern.CASE_INSENSITIVE));
	}

	private <T> void loadBlacklist(GPExtraProtections plugin, Set<T> blacklist,
			String identifier, Function<String, T> loadFunction) {
		blacklist.clear();

		List<String> list = plugin.getConfig().getStringList("features.protect-nametagged-entities." + identifier);

		if (list.isEmpty()) {
			return;
		}

		blacklist.addAll(list.stream().filter(Objects::nonNull).map(value -> {
			try {
				return loadFunction.apply(value);
			} catch (Exception e) {
				plugin.getLogger().warning(String.format("[ProtectNametagged] Invalid value in %s: %s", identifier, value));
				plugin.getLogger().warning(String.format("%s cause: %s", e.getClass().getSimpleName(), e.getMessage()));
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList()));
	}

	@EventHandler(ignoreCancelled = true)
	private void protectNametaggedEntity(EntityDamageByEntityEvent event) {
		// Enforce entity type blacklist.
		if (typeBlacklist.contains(event.getEntityType())) return;

		// Only protect living entities.
		if (!(event.getEntity() instanceof LivingEntity)) return;

		LivingEntity entity = (LivingEntity) event.getEntity();

		// Ensure name is set.
		if (entity.getCustomName() == null) return;

		// Ensure entity has actually been tagged by a player - entity will not despawn and name is visible.
		if (entity.getRemoveWhenFarAway() || !entity.isCustomNameVisible()) return;

		// Enforce persistent data key blacklist.
		PersistentDataContainer container = entity.getPersistentDataContainer();
		if (!container.isEmpty() && keyBlacklist.stream().anyMatch(key -> isKeyPresent(container, key))) return;

		// Enforce metadata blacklist.
		if (metaBlacklist.stream().anyMatch(entity::hasMetadata)) return;

		// Enforce name pattern blacklist.
		if (nameBlacklist.stream().anyMatch(pattern -> pattern.matcher(entity.getCustomName()).find())) return;

		GPExtraProtections.blockIfClaimed(event, entity.getLocation(), GPExtraProtections.getAttackingPlayer(event.getDamager()));

	}

	private boolean isKeyPresent(PersistentDataContainer container, NamespacedKey key) {
		try {
			return container.get(key, PersistentDataType.BYTE) != null;
		} catch (IllegalArgumentException e) {
			// Data is present under another type.
			return true;
		}
	}

}
