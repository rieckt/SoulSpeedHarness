package com.soulspeedharness;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SoulSpeedHarnessPlugin extends JavaPlugin implements Listener {

    private NamespacedKey soulSpeedModifierKey;

    private static final int SPEED_UPDATE_INTERVAL_TICKS = 5;
    private static final int ANVIL_RESULT_SLOT = 2;
    private static final int BASE_ENCHANT_COST = 2;
    private static final int COST_PER_ENCHANTMENT = 1;
    private static final int MAX_REPAIR_COST = 39;
    private static final double SOUL_SPEED_BASE_BOOST = 0.40;
    private static final double SOUL_SPEED_BOOST_PER_LEVEL = 0.40;

    private Enchantment soulSpeedEnchantment;

    @Override
    public void onEnable() {
        soulSpeedModifierKey = new NamespacedKey(this, "soul_speed_modifier");

        if (!initializeSoulSpeedEnchantment()) {
            getLogger().severe("Soul Speed enchantment not found! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (player.isInsideVehicle()) {
                        Entity vehicle = player.getVehicle();
                        if (vehicle != null && isHappyGhast(vehicle)) {
                            updateGhastSpeed((LivingEntity) vehicle);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, SPEED_UPDATE_INTERVAL_TICKS);

        getLogger().info("SoulSpeedHarness has been enabled!");
    }

    /**
     * Initializes the Soul Speed enchantment reference.
     * This is cached to avoid repeated lookups during runtime.
     *
     * @return true if enchantment was found and initialized, false otherwise
     */
    private boolean initializeSoulSpeedEnchantment() {
        @SuppressWarnings("deprecation")
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("soul_speed"));
        if (enchantment == null) {
            return false;
        }
        this.soulSpeedEnchantment = enchantment;
        return true;
    }

    @Override
    public void onDisable() {
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
        if (event.getEntity() instanceof Player) {
            Entity mount = event.getMount();
            if (isHappyGhast(mount)) {
                updateGhastSpeed((LivingEntity) mount);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Entity dismounted = event.getDismounted();
            if (isHappyGhast(dismounted)) {
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
     * Handles the PrepareAnvilEvent to allow Soul Speed enchantment to be applied
     * to harnesses via anvil, even though it's normally only compatible with boots.
     *
     * @param event The PrepareAnvilEvent triggered when anvil recipe is prepared
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (soulSpeedEnchantment == null) {
            return;
        }

        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getFirstItem();
        ItemStack secondItem = inventory.getSecondItem();

        if (!isValidAnvilCombination(firstItem, secondItem)) {
            return;
        }

        int soulSpeedLevel = extractSoulSpeedLevel(secondItem);
        if (soulSpeedLevel <= 0) {
            return;
        }

        ItemStack result = createEnchantedHarness(firstItem, secondItem, soulSpeedLevel);
        if (result != null) {
            int repairCost = calculateAnvilCost(firstItem, secondItem, soulSpeedLevel);
            setRepairCostOnItem(result, repairCost);
            event.setResult(result);

            if (event.getView() instanceof AnvilView anvilView) {
                anvilView.setRepairCost(repairCost);
            }
        }
    }

    /**
     * Sets the repair cost on the result item meta for persistence.
     */
    private void setRepairCostOnItem(ItemStack result, int repairCost) {
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta instanceof org.bukkit.inventory.meta.Repairable repairable) {
            repairable.setRepairCost(repairCost);
            result.setItemMeta(resultMeta);
        }
    }

    /**
     * Checks if the anvil combination is valid (harness + enchanted book).
     *
     * @param firstItem  The first item in the anvil
     * @param secondItem The second item in the anvil
     * @return true if combination is valid, false otherwise
     */
    private boolean isValidAnvilCombination(ItemStack firstItem, ItemStack secondItem) {
        return firstItem != null
                && isHarness(firstItem.getType())
                && secondItem != null
                && secondItem.getType() == Material.ENCHANTED_BOOK;
    }

    /**
     * Extracts the Soul Speed level from an enchanted book.
     *
     * @param book The enchanted book item
     * @return The Soul Speed level, or 0 if not found
     */
    private int extractSoulSpeedLevel(ItemStack book) {
        if (book == null || soulSpeedEnchantment == null) {
            return 0;
        }

        ItemMeta bookMeta = book.getItemMeta();
        if (!(bookMeta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta)) {
            return 0;
        }

        return storageMeta.getStoredEnchantLevel(soulSpeedEnchantment);
    }

    /**
     * Creates an enchanted harness by combining a harness with an enchanted book.
     *
     * @param harness        The original harness item
     * @param book           The enchanted book containing Soul Speed
     * @param soulSpeedLevel The Soul Speed level from the book
     * @return The resulting enchanted harness, or null if creation fails
     */
    private ItemStack createEnchantedHarness(ItemStack harness, ItemStack book, int soulSpeedLevel) {
        if (harness == null || book == null || soulSpeedEnchantment == null) {
            return null;
        }

        ItemStack result = harness.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) {
            return null;
        }

        int currentLevel = resultMeta.getEnchantLevel(soulSpeedEnchantment);
        int newLevel = Math.max(currentLevel, soulSpeedLevel);
        resultMeta.addEnchant(soulSpeedEnchantment, newLevel, true);

        if (book.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
            copyOtherEnchantments(resultMeta, storageMeta);
        }

        result.setItemMeta(resultMeta);
        return result;
    }

    /**
     * Calculates the XP cost for applying Soul Speed to a harness via anvil. The cost is based on:
     * - Base cost for enchanting (2 levels) - Soul Speed level cost (1 level per enchantment level)
     * - Additional enchantments from the book (1 level per enchantment) - Repair cost from the
     * original harness (if any)
     *
     * @param harness The original harness item
     * @param book The enchanted book
     * @param soulSpeedLevel The Soul Speed level being applied
     * @return The total repair cost in levels
     */
    private int calculateAnvilCost(ItemStack harness, ItemStack book, int soulSpeedLevel) {
        int cost = BASE_ENCHANT_COST + soulSpeedLevel;
        cost += calculateAdditionalEnchantmentCost(book);
        cost += calculatePreviousRepairCost(harness);
        return Math.min(cost, MAX_REPAIR_COST);
    }

    /**
     * Calculates the cost for additional enchantments from the book.
     *
     * @param book The enchanted book
     * @return The additional cost in levels
     */
    private int calculateAdditionalEnchantmentCost(ItemStack book) {
        if (!(book.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta)) {
            return 0;
        }

        int additionalCost = 0;
        for (Enchantment enchant : storageMeta.getStoredEnchants().keySet()) {
            if (enchant != soulSpeedEnchantment && storageMeta.getStoredEnchantLevel(enchant) > 0) {
                additionalCost += COST_PER_ENCHANTMENT;
            }
        }
        return additionalCost;
    }

    /**
     * Calculates the cost from previous repair operations on the harness.
     *
     * @param harness The original harness item
     * @return The previous repair cost contribution
     */
    private int calculatePreviousRepairCost(ItemStack harness) {
        if (harness == null || !harness.hasItemMeta()) {
            return 0;
        }

        ItemMeta harnessMeta = harness.getItemMeta();
        if (!(harnessMeta instanceof org.bukkit.inventory.meta.Repairable repairable)) {
            return 0;
        }

        int previousCost = repairable.getRepairCost();
        return previousCost > 0 ? Math.min(previousCost, MAX_REPAIR_COST) : 0;
    }

    /**
     * Copies enchantments from an enchanted book to the result item meta,
     * excluding Soul Speed which is handled separately.
     *
     * @param resultMeta  The target item meta
     * @param storageMeta The source enchantment storage meta
     */
    private void copyOtherEnchantments(ItemMeta resultMeta,
            org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
        for (Enchantment enchant : storageMeta.getStoredEnchants().keySet()) {
            if (enchant != soulSpeedEnchantment) {
                int bookLevel = storageMeta.getStoredEnchantLevel(enchant);
                if (bookLevel > 0) {
                    int existingLevel = resultMeta.getEnchantLevel(enchant);
                    int finalLevel = Math.max(existingLevel, bookLevel);

                    if (finalLevel > 0) {
                        try {
                            resultMeta.addEnchant(enchant, finalLevel, true);
                        } catch (Exception e) {
                            getLogger().fine("Could not add enchantment " + enchant.getKey()
                                    + " to harness: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles clicking on the result slot in an anvil to allow taking harnesses
     * with Soul Speed, bypassing the default validation that prevents it.
     *
     * @param event The InventoryClickEvent triggered when clicking in anvil
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        if (event.getSlot() != ANVIL_RESULT_SLOT) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        AnvilInventory anvil = (AnvilInventory) event.getInventory();
        ItemStack result = anvil.getResult();

        if (!isValidHarnessResult(result)) {
            return;
        }

        event.setCancelled(true);
        processAnvilResult(event, player, anvil, result);
    }

    /**
     * Checks if the result item is a valid harness with Soul Speed.
     *
     * @param result The result item to check
     * @return true if valid, false otherwise
     */
    private boolean isValidHarnessResult(ItemStack result) {
        if (result == null || !isHarness(result.getType())) {
            return false;
        }

        int soulSpeedLevel = getSoulSpeedLevel(result);
        return soulSpeedLevel > 0;
    }

    /**
     * Processes the anvil result by giving it to the player and consuming input
     * items.
     *
     * @param event  The inventory click event
     * @param player The player who clicked
     * @param anvil  The anvil inventory
     * @param result The result item to give
     */
    private void processAnvilResult(InventoryClickEvent event, Player player,
            AnvilInventory anvil, ItemStack result) {
        int repairCost = getRepairCostFromAnvil(event);

        if (repairCost > 0 && !deductPlayerExperience(player, repairCost)) {
            return;
        }

        giveItemToPlayer(player, result.clone());
        consumeAnvilInputs(anvil);
        anvil.setResult(null);
        player.updateInventory();
    }

    /**
     * Gets the repair cost from the anvil view using Paper 1.21+ API.
     */
    private int getRepairCostFromAnvil(InventoryClickEvent event) {
        if (event.getView() instanceof AnvilView anvilView) {
            return anvilView.getRepairCost();
        }
        return 0;
    }

    /**
     * Deducts experience from the player if they have enough.
     *
     * @param player     The player to deduct experience from
     * @param repairCost The cost in levels
     * @return true if experience was deducted, false if player doesn't have enough
     */
    private boolean deductPlayerExperience(Player player, int repairCost) {
        int playerLevel = player.getLevel();
        if (playerLevel < repairCost) {
            player.sendMessage("§cYou don't have enough experience! Required: " + repairCost + " levels");
            return false;
        }

        player.setLevel(playerLevel - repairCost);
        return true;
    }

    /**
     * Gives an item to the player's inventory. Uses scheduler to safely modify inventory after
     * event cancellation, avoiding deprecated setCursor API that can cause inconsistencies.
     *
     * @param player The player to give the item to
     * @param item The item to give
     */
    private void giveItemToPlayer(Player player, ItemStack item) {
        getServer().getScheduler().runTask(this, () -> {
            var remaining = player.getInventory().addItem(item);
            if (!remaining.isEmpty()) {
                for (ItemStack dropped : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                }
            }
            player.updateInventory();
        });
    }

    /**
     * Consumes the input items from the anvil (decrements amount or removes).
     *
     * @param anvil The anvil inventory
     */
    private void consumeAnvilInputs(AnvilInventory anvil) {
        consumeFirstItem(anvil);
        consumeSecondItem(anvil);
    }

    /**
     * Consumes the first item from the anvil.
     *
     * @param anvil The anvil inventory
     */
    private void consumeFirstItem(AnvilInventory anvil) {
        consumeAnvilItem(anvil, true);
    }

    /**
     * Consumes the second item from the anvil.
     *
     * @param anvil The anvil inventory
     */
    private void consumeSecondItem(AnvilInventory anvil) {
        consumeAnvilItem(anvil, false);
    }

    /**
     * Consumes an item from the anvil (first or second slot).
     *
     * @param anvil The anvil inventory
     * @param isFirst true for first slot, false for second slot
     */
    private void consumeAnvilItem(AnvilInventory anvil, boolean isFirst) {
        ItemStack item = isFirst ? anvil.getFirstItem() : anvil.getSecondItem();
        if (item == null) {
            return;
        }

        if (item.getAmount() > 1) {
            ItemStack newItem = item.clone();
            newItem.setAmount(item.getAmount() - 1);
            if (isFirst) {
                anvil.setFirstItem(newItem);
            } else {
                anvil.setSecondItem(newItem);
            }
        } else {
            if (isFirst) {
                anvil.setFirstItem(null);
            } else {
                anvil.setSecondItem(null);
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

    /**
     * Updates the speed of a ghast based on the harness's Soul Speed enchantment.
     * In Minecraft 1.21+, harnesses are stored in the body equipment slot.
     */
    private void updateGhastSpeed(LivingEntity ghast) {
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

        double speedBoost = calculateSpeedBoost(soulSpeedLevel);
        applySpeedModifier(ghast, speedBoost);
    }

    /**
     * Calculates the speed boost based on Soul Speed level.
     * Formula: base 0.40 + 0.40 per level above first.
     * Level 1: 0.40 (40%), Level 2: 0.80 (80%), Level 3: 1.20 (120%)
     */
    private double calculateSpeedBoost(int soulSpeedLevel) {
        return SOUL_SPEED_BASE_BOOST + (soulSpeedLevel - 1) * SOUL_SPEED_BOOST_PER_LEVEL;
    }

    /**
     * Gets the harness from an entity's body equipment slot.
     * In Minecraft 1.21+, harnesses are stored in the body equipment slot.
     */
    private ItemStack getHarness(LivingEntity entity) {
        if (entity.getEquipment() == null) {
            return null;
        }
        return entity.getEquipment().getItem(EquipmentSlot.BODY);
    }

    /**
     * Gets the Soul Speed enchantment level from an item.
     *
     * @param item The item to check
     * @return The Soul Speed level, or 0 if not found
     */
    private int getSoulSpeedLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || soulSpeedEnchantment == null) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        return meta.getEnchantLevel(soulSpeedEnchantment);
    }

    /**
     * Applies a speed modifier to an entity using the flying speed attribute.
     * Uses NamespacedKey instead of deprecated UUID constructor.
     */
    private void applySpeedModifier(LivingEntity entity, double speedBoost) {
        AttributeInstance attribute = entity.getAttribute(Attribute.FLYING_SPEED);
        if (attribute == null) {
            return;
        }

        removeSpeedModifier(entity);

        AttributeModifier modifier = new AttributeModifier(
                soulSpeedModifierKey,
                speedBoost,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        attribute.addModifier(modifier);
    }

    /**
     * Removes the speed modifier from an entity.
     * Uses NamespacedKey instead of deprecated UUID method.
     */
    private void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.FLYING_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(soulSpeedModifierKey);
    }
}
