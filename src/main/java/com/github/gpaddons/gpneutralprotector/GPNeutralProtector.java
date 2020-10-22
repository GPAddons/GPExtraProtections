package com.github.gpaddons.gpneutralprotector;

import com.github.jikoo.util.event.Event;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public class GPNeutralProtector extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadFeatures();
	}

	public void reloadFeatures() {
		HandlerList.unregisterAll(this);

		if (getConfig().getBoolean("features.protect-unarmed-pillagers")) {
			Event.register(EntityDamageByEntityEvent.class, this::protectUnarmedPillager, this);
		}
	}

	private void protectUnarmedPillager(EntityDamageByEntityEvent event) {
		if (event.getEntityType() != EntityType.PILLAGER) return;

		Pillager pillager = (Pillager) event.getEntity();
		EntityEquipment equipment = pillager.getEquipment();

		if (equipment != null && (equipment.getItemInMainHand().getType() != Material.AIR || equipment.getItemInOffHand().getType() != Material.AIR)) return;

		Entity damager = event.getDamager();
		Player attacker = null;
		if (damager instanceof Player) {
			attacker = (Player) damager;
		} else if (damager instanceof Projectile) {
			ProjectileSource shooter = ((Projectile) damager).getShooter();
			if (shooter instanceof Player) {
				attacker = (Player) shooter;
			}
		}

		Claim cachedClaim = null;
		PlayerData playerData = null;
		if (attacker != null) {
			playerData = GriefPrevention.instance.dataStore.getPlayerData(attacker.getUniqueId());
			cachedClaim = playerData.lastClaim;
		}

		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);

		if (claim == null) return;

		if (attacker == null) {
			event.setCancelled(true);
			return;
		}

		String failureReason = claim.allowBuild(attacker, Material.AIR);

		if (failureReason == null) return;

		event.setCancelled(true);
		GriefPrevention.sendMessage(attacker, ChatColor.RED, failureReason);
		playerData.lastClaim = claim;
	}

}
