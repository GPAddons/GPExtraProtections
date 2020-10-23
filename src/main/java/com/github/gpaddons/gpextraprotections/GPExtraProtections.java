package com.github.gpaddons.gpextraprotections;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GPExtraProtections extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadFeatures();
	}

	public void reloadFeatures() {
		HandlerList.unregisterAll(this);

		if (getConfig().getBoolean("features.protect-unarmed-pillagers")) {
			getServer().getPluginManager().registerEvents(new ProtectUnarmedPillager(), this);
		}

		if (getConfig().getBoolean("features.protect-nametagged-entities.enabled")) {
			getServer().getPluginManager().registerEvents(new ProtectNametaggedEntities(this), this);
		}
	}

	@Contract("null -> null")
	static @Nullable Player getAttackingPlayer(@Nullable Entity damager) {
		if (damager instanceof Player) {
			return  (Player) damager;
		}

		if (damager instanceof Projectile) {
			ProjectileSource shooter = ((Projectile) damager).getShooter();
			if (shooter instanceof Player) {
				return  (Player) shooter;
			}
		}

		return null;
	}

	static void blockIfClaimed(@NotNull Cancellable cancellable, @NotNull Location target, @Nullable Player actor) {
		// Use cached claim where possible.
		Claim cachedClaim = null;
		PlayerData playerData = null;
		if (actor != null) {
			playerData = GriefPrevention.instance.dataStore.getPlayerData(actor.getUniqueId());
			cachedClaim = playerData.lastClaim;
		}

		// Get current claim.
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(target, false, cachedClaim);

		// Only protect inside claims.
		if (claim == null) return;

		// Always block non-player attacks.
		if (actor == null) {
			cancellable.setCancelled(true);
			return;
		}

		String failureReason = claim.allowBuild(actor, Material.AIR);

		// Only block players without build trust.
		if (failureReason == null) return;

		cancellable.setCancelled(true);
		GriefPrevention.sendMessage(actor, ChatColor.RED, failureReason);
		playerData.lastClaim = claim;
	}

}
