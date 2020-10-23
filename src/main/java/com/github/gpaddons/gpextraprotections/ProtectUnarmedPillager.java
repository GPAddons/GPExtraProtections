package com.github.gpaddons.gpextraprotections;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.NotNull;

public class ProtectUnarmedPillager implements Listener {

	@EventHandler
	private void protectUnarmedPillager(@NotNull EntityDamageByEntityEvent event) {
		// Only protect pillagers.
		if (event.getEntityType() != EntityType.PILLAGER) return;

		Pillager pillager = (Pillager) event.getEntity();
		EntityEquipment equipment = pillager.getEquipment();

		// Only protect if pillager has no weapons.
		if (equipment != null && (equipment.getItemInMainHand().getType() != Material.AIR || equipment.getItemInOffHand().getType() != Material.AIR)) return;

		GPExtraProtections.blockIfClaimed(event, pillager.getLocation(), GPExtraProtections.getAttackingPlayer(event.getDamager()));
	}

}
