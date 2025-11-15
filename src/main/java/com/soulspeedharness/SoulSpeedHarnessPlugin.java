package com.soulspeedharness;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SoulSpeedHarnessPlugin extends JavaPlugin implements Listener {

    private static final NamespacedKey SOUL_SPEED_MODIFIER_KEY = new NamespacedKey("soulspeedharness",
            "soul_speed_modifier");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Periodically update speed for mounted players
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (player.isInsideVehicle()) {
                        Entity vehicle = player.getVehicle();
                        if (vehicle != null && isHappyGhast(vehicle)) {
                            updateGhastSpeed((LivingEntity) vehicle, player);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L); // Check every 5 ticks (0.25 seconds)

        getLogger().info("SoulSpeedHarness has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up all modifiers when plugin disables
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.isInsideVehicle()) {
                Entity vehicle = player.getVehicle();
                if (vehicle != null && isHappyGhast(vehicle)) {
                    removeSpeedModifier((LivingEntity) vehicle);
                }
            }
        }
        getLogger().info("SoulSpeedHarness has been disabled!");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player player) {
            Entity mount = event.getMount();
            if (mount != null && isHappyGhast(mount)) {
                updateGhastSpeed((LivingEntity) mount, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Entity dismounted = event.getDismounted();
            if (dismounted != null && isHappyGhast(dismounted)) {
                removeSpeedModifier((LivingEntity) dismounted);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().isInsideVehicle()) {
            Entity vehicle = event.getPlayer().getVehicle();
            if (vehicle != null && isHappyGhast(vehicle)) {
                removeSpeedModifier((LivingEntity) vehicle);
            }
        }
    }

    /**
     * Check if an entity is a Happy Ghast (or regular Ghast for compatibility)
     */
    private boolean isHappyGhast(Entity entity) {
        EntityType type = entity.getType();
        return type == EntityType.HAPPY_GHAST || type == EntityType.GHAST;
    }

    /**
     * Check if a Material is a harness (any color)
     */
    private boolean isHarness(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_HARNESS");
    }

    private void updateGhastSpeed(LivingEntity ghast, Player player) {
        // Check if ghast has a harness (saddle) with Soul Speed
        ItemStack harness = getHarness(ghast);
        if (harness == null || !isHarness(harness.getType())) {
            removeSpeedModifier(ghast);
            return;
        }

        int soulSpeedLevel = getSoulSpeedLevel(harness);
        if (soulSpeedLevel <= 0) {
            removeSpeedModifier(ghast);
            return;
        }

        // Calculate speed boost: base 0.40 + 0.40 per level above first
        // Level 1: 0.40 (40%), Level 2: 0.80 (80%), Level 3: 1.20 (120%)
        double speedBoost = 0.40 + (soulSpeedLevel - 1) * 0.40;

        // Check if sprinting for additional boost
        // Note: When riding a Ghast, player is always in the air, so we only check
        // sprinting
        boolean isSprinting = player.isSprinting();
        if (isSprinting) {
            // Sprint boost: same as normal (base 0.40 + 0.40 per level above first)
            // Level 1: 0.40 (40%), Level 2: 0.80 (80%), Level 3: 1.20 (120%)
            speedBoost = 0.40 + (soulSpeedLevel - 1) * 0.40;
        }

        applySpeedModifier(ghast, speedBoost);
    }

    private ItemStack getHarness(LivingEntity entity) {
        // In Minecraft 1.21+, harnesses are stored in the body equipment slot
        // For rideable entities like Happy Ghasts, check the body slot
        if (entity.getEquipment() == null) {
            return null;
        }
        return entity.getEquipment().getItem(EquipmentSlot.BODY);
    }

    private int getSoulSpeedLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        // Check for Soul Speed enchantment using Registry API (replaces deprecated
        // getByKey)
        Enchantment soulSpeed = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("soul_speed"));
        if (soulSpeed == null) {
            return 0;
        }
        return meta.getEnchantLevel(soulSpeed);
    }

    private void applySpeedModifier(LivingEntity entity, double speedBoost) {
        // Use flying speed attribute for Happy Ghasts
        AttributeInstance attribute = entity.getAttribute(Attribute.FLYING_SPEED);
        if (attribute == null) {
            return;
        }

        // Remove existing modifier if present
        removeSpeedModifier(entity);

        // Add new modifier with multiply_base operation using NamespacedKey (replaces
        // deprecated UUID constructor)
        AttributeModifier modifier = new AttributeModifier(
                SOUL_SPEED_MODIFIER_KEY,
                speedBoost,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        attribute.addModifier(modifier);

        // Mark in PDC that modifier is applied
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(SOUL_SPEED_MODIFIER_KEY, PersistentDataType.DOUBLE, speedBoost);
    }

    private void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.FLYING_SPEED);
        if (attribute == null) {
            return;
        }

        // Remove modifier by NamespacedKey (replaces deprecated UUID method)
        try {
            attribute.removeModifier(SOUL_SPEED_MODIFIER_KEY);
        } catch (Exception e) {
            // Modifier might not exist, ignore
        }

        // Clear PDC marker
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.remove(SOUL_SPEED_MODIFIER_KEY);
    }
}
