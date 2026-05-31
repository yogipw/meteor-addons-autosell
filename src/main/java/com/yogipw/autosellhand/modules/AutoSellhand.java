package com.yogipw.autosellhand.modules;

import com.yogipw.autosellhand.AutoSellhandAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class AutoSellhand extends Module {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    private final SettingGroup sgSelling = settings.createGroup("Selling");
    private final SettingGroup sgToolRepair = settings.createGroup("Tool Repair");
    private final SettingGroup sgArmorRepair = settings.createGroup("Armor Repair");

    // Selling
    private final Setting<Boolean> autoSell = sgSelling.add(new BoolSetting.Builder()
        .name("auto-sell")
        .description("Automatically sell whitelisted mining items when the inventory is full.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Item>> sellWhitelist = sgSelling.add(new ItemListSetting.Builder()
        .name("sell-whitelist")
        .description("Only these items may be sold.")
        .defaultValue(
            Items.COAL,
            Items.RAW_COPPER,
            Items.RAW_IRON,
            Items.RAW_GOLD,
            Items.REDSTONE,
            Items.LAPIS_LAZULI,
            Items.DIAMOND,
            Items.EMERALD
        )
        .visible(autoSell::get)
        .build()
    );

    private final Setting<String> sellCommand = sgSelling.add(new StringSetting.Builder()
        .name("sell-command")
        .description("The sell command. A leading slash is optional.")
        .defaultValue("/sellhand")
        .visible(autoSell::get)
        .build()
    );

    private final Setting<Integer> sellHotbarSlot = sgSelling.add(new IntSetting.Builder()
        .name("sell-hotbar-slot")
        .description("The hotbar slot used to hold a stack before selling.")
        .defaultValue(9)
        .range(1, 9)
        .sliderRange(1, 9)
        .visible(autoSell::get)
        .build()
    );

    private final Setting<Integer> sellDelayTicks = sgSelling.add(new IntSetting.Builder()
        .name("sell-delay-ticks")
        .description("Ticks to wait after each sell command. Twenty ticks are about one second.")
        .defaultValue(20)
        .range(1, 200)
        .sliderRange(1, 100)
        .visible(autoSell::get)
        .build()
    );

    private final Setting<Boolean> restoreSelectedSlot = sgSelling.add(new BoolSetting.Builder()
        .name("restore-selected-slot")
        .description("Return to the previously selected hotbar slot when selling stops.")
        .defaultValue(true)
        .visible(autoSell::get)
        .build()
    );

    // Tool repair
    private final Setting<Boolean> autoRepairTools = sgToolRepair.add(new BoolSetting.Builder()
        .name("auto-repair-tools")
        .description("Automatically repair configured tools in the hotbar one at a time.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoRepairArmor = sgArmorRepair.add(new BoolSetting.Builder()
        .name("auto-repair-armor")
        .description("Automatically repair equipped armor one piece at a time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> repairItems = sgToolRepair.add(new ItemListSetting.Builder()
        .name("repair-items")
        .description("Hotbar items that may be repaired automatically.")
        .defaultValue(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE,
            Items.WOODEN_SHOVEL,
            Items.STONE_SHOVEL,
            Items.IRON_SHOVEL,
            Items.GOLDEN_SHOVEL,
            Items.DIAMOND_SHOVEL,
            Items.NETHERITE_SHOVEL,
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.IRON_AXE,
            Items.GOLDEN_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE,
            Items.WOODEN_HOE,
            Items.STONE_HOE,
            Items.IRON_HOE,
            Items.GOLDEN_HOE,
            Items.DIAMOND_HOE,
            Items.NETHERITE_HOE,
            Items.SHEARS
        )
        .visible(autoRepairTools::get)
        .build()
    );

    private final Setting<String> repairCommand = sgToolRepair.add(new StringSetting.Builder()
        .name("repair-command")
        .description("The repair command. A leading slash is optional.")
        .defaultValue("/repair")
        .visible(() -> autoRepairTools.get() || autoRepairArmor.get())
        .build()
    );

    private final Setting<Integer> toolDurabilityPercent = sgToolRepair.add(new IntSetting.Builder()
        .name("tool-durability-percent")
        .description("Repair tools when their remaining durability percentage is at or below this value.")
        .defaultValue(50)
        .range(1, 99)
        .sliderRange(1, 99)
        .visible(autoRepairTools::get)
        .build()
    );

    private final Setting<Integer> repairDelayTicks = sgToolRepair.add(new IntSetting.Builder()
        .name("repair-delay-ticks")
        .description("Ticks to wait after each repair command. Twenty ticks are about one second.")
        .defaultValue(60)
        .range(20, 1200)
        .sliderRange(20, 400)
        .visible(() -> autoRepairTools.get() || autoRepairArmor.get())
        .build()
    );

    // Armor repair
    private final Setting<Integer> armorDurabilityPercent = sgArmorRepair.add(new IntSetting.Builder()
        .name("armor-durability-percent")
        .description("Repair armor when its remaining durability percentage is at or below this value.")
        .defaultValue(50)
        .range(1, 99)
        .sliderRange(1, 99)
        .visible(autoRepairArmor::get)
        .build()
    );

    private final Setting<Integer> armorRepairHotbarSlot = sgArmorRepair.add(new IntSetting.Builder()
        .name("armor-repair-hotbar-slot")
        .description("Temporary hotbar slot used while repairing armor.")
        .defaultValue(8)
        .range(1, 9)
        .sliderRange(1, 9)
        .visible(autoRepairArmor::get)
        .build()
    );

    private int sellCooldown;
    private int sellPreviousSelectedSlot = -1;
    private boolean warnedInvalidSellCommand;

    private int repairCooldown;
    private RepairType pendingRepair = RepairType.None;
    private int repairPreviousSelectedSlot = -1;
    private int pendingArmorSlotId = -1;
    private int pendingArmorHotbarSlot = -1;
    private int displacedArmorHotbarItemSlot = -1;
    private boolean repairSession;
    private boolean resumeBaritone;
    private boolean warnedInvalidRepairCommand;
    private boolean warnedNoArmorStorage;

    public AutoSellhand() {
        super(AutoSellhandAddon.CATEGORY, "auto-sellhand", "Automatically sells mining drops and repairs hotbar tools or equipped armor.");
    }

    @Override
    public void onActivate() {
        sellCooldown = 0;
        sellPreviousSelectedSlot = -1;
        warnedInvalidSellCommand = false;
        resetRepairState();
    }

    @Override
    public void onDeactivate() {
        restoreSellSlot();
        finishPendingRepair();
        finishRepairSession();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (handleRepair()) return;
        handleSelling();
    }

    private boolean handleRepair() {
        if (repairCooldown > 0) {
            repairCooldown--;
            return true;
        }

        if (pendingRepair != RepairType.None) {
            if (!isPlayerInventoryOpen()) return true;
            finishPendingRepair();
        }

        if (!isPlayerInventoryOpen()) {
            if (repairSession) finishRepairSession();
            return false;
        }

        int toolSlot = autoRepairTools.get() ? findRepairToolSlot() : -1;
        EquipmentSlot armorSlot = autoRepairArmor.get() ? findRepairArmorSlot() : null;

        if (toolSlot == -1 && armorSlot == null) {
            finishRepairSession();
            return false;
        }

        String command = normalizedCommand(repairCommand.get());
        if (command == null) {
            if (!warnedInvalidRepairCommand) {
                warning("The repair command setting is empty. Auto repair is waiting for a valid command.");
                warnedInvalidRepairCommand = true;
            }
            finishRepairSession();
            return false;
        }
        warnedInvalidRepairCommand = false;

        startRepairSession();

        if (toolSlot != -1) {
            startToolRepair(toolSlot, command);
            return true;
        }

        if (!startArmorRepair(armorSlot, command)) {
            finishRepairSession();
        }
        return true;
    }

    private void startToolRepair(int toolSlot, String command) {
        repairPreviousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.swap(toolSlot, false);
        ChatUtils.sendPlayerMsg(command, false);
        pendingRepair = RepairType.Tool;
        repairCooldown = repairDelayTicks.get();
    }

    private boolean startArmorRepair(EquipmentSlot armorSlot, String command) {
        int hotbarSlot = armorRepairHotbarSlot.get() - 1;
        ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);
        displacedArmorHotbarItemSlot = -1;

        if (!hotbarStack.isEmpty()) {
            displacedArmorHotbarItemSlot = findEmptyMainInventorySlot();
            if (displacedArmorHotbarItemSlot == -1) {
                if (!warnedNoArmorStorage) {
                    warning("Armor repair needs an empty main-inventory slot to store the configured hotbar item.");
                    warnedNoArmorStorage = true;
                }
                return false;
            }
            InvUtils.move().fromHotbar(hotbarSlot).to(displacedArmorHotbarItemSlot);
        }
        warnedNoArmorStorage = false;

        pendingArmorSlotId = armorSlot.getEntitySlotId();
        pendingArmorHotbarSlot = hotbarSlot;
        repairPreviousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.move().fromArmor(pendingArmorSlotId).toHotbar(hotbarSlot);

        ItemStack preparedArmor = mc.player.getInventory().getStack(hotbarSlot);
        if (!needsRepair(preparedArmor, armorDurabilityPercent.get())) {
            restorePendingArmor();
            restoreRepairSelectedSlot();
            return false;
        }

        InvUtils.swap(hotbarSlot, false);
        ChatUtils.sendPlayerMsg(command, false);
        pendingRepair = RepairType.Armor;
        repairCooldown = repairDelayTicks.get();
        return true;
    }

    private void finishPendingRepair() {
        if (pendingRepair == RepairType.Armor) restorePendingArmor();
        restoreRepairSelectedSlot();
        pendingRepair = RepairType.None;
    }

    private void restorePendingArmor() {
        if (pendingArmorSlotId == -1 || mc.player == null || mc.interactionManager == null) return;

        InvUtils.move().fromHotbar(pendingArmorHotbarSlot).toArmor(pendingArmorSlotId);
        pendingArmorSlotId = -1;

        if (displacedArmorHotbarItemSlot != -1) {
            InvUtils.move().from(displacedArmorHotbarItemSlot).toHotbar(pendingArmorHotbarSlot);
            displacedArmorHotbarItemSlot = -1;
        }
        pendingArmorHotbarSlot = -1;
    }

    private void restoreRepairSelectedSlot() {
        if (repairPreviousSelectedSlot == -1 || mc.player == null || mc.interactionManager == null) return;

        InvUtils.swap(repairPreviousSelectedSlot, false);
        repairPreviousSelectedSlot = -1;
    }

    private void startRepairSession() {
        if (repairSession) return;

        repairSession = true;
        resumeBaritone = PathManagers.get().isPathing();
        if (resumeBaritone) PathManagers.get().pause();
    }

    private void finishRepairSession() {
        if (!repairSession) return;

        if (resumeBaritone) PathManagers.get().resume();
        repairSession = false;
        resumeBaritone = false;
    }

    private int findRepairToolSlot() {
        for (int slot = SlotUtils.HOTBAR_START; slot <= SlotUtils.HOTBAR_END; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (repairItems.get().contains(stack.getItem()) && needsRepair(stack, toolDurabilityPercent.get())) {
                return slot;
            }
        }

        return -1;
    }

    private EquipmentSlot findRepairArmorSlot() {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (needsRepair(mc.player.getEquippedStack(slot), armorDurabilityPercent.get())) {
                return slot;
            }
        }

        return null;
    }

    private boolean needsRepair(ItemStack stack, int thresholdPercent) {
        if (stack.isEmpty() || !stack.isDamageable()) return false;
        return (stack.getMaxDamage() - stack.getDamage()) * 100 <= stack.getMaxDamage() * thresholdPercent;
    }

    private int findEmptyMainInventorySlot() {
        for (int slot = SlotUtils.MAIN_START; slot <= SlotUtils.MAIN_END; slot++) {
            if (mc.player.getInventory().getStack(slot).isEmpty()) return slot;
        }

        return -1;
    }

    private void handleSelling() {
        if (!autoSell.get()) {
            restoreSellSlot();
            return;
        }

        if (sellCooldown > 0) {
            sellCooldown--;
            return;
        }

        // Slot IDs differ for other containers, so wait until they are closed.
        if (!isPlayerInventoryOpen()) return;

        if (!isInventoryFull()) {
            restoreSellSlot();
            return;
        }

        String command = normalizedCommand(sellCommand.get());
        if (command == null) {
            if (!warnedInvalidSellCommand) {
                warning("The sell command setting is empty. Auto SellHand is waiting for a valid command.");
                warnedInvalidSellCommand = true;
            }
            return;
        }
        warnedInvalidSellCommand = false;

        int targetSlot = sellHotbarSlot.get() - 1;
        int sourceSlot = findSellableStack(targetSlot);
        if (sourceSlot == -1) {
            restoreSellSlot();
            return;
        }

        if (sellPreviousSelectedSlot == -1) {
            sellPreviousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        }

        if (sourceSlot != targetSlot) {
            InvUtils.move().from(sourceSlot).toHotbar(targetSlot);
        }

        ItemStack targetStack = mc.player.getInventory().getStack(targetSlot);
        if (!isSellWhitelisted(targetStack)) {
            warning("Could not prepare the configured sell hotbar slot. Auto SellHand will retry.");
            restoreSellSlot();
            sellCooldown = sellDelayTicks.get();
            return;
        }

        InvUtils.swap(targetSlot, false);
        ChatUtils.sendPlayerMsg(command, false);
        sellCooldown = sellDelayTicks.get();
    }

    private boolean isInventoryFull() {
        for (int slot = SlotUtils.HOTBAR_START; slot <= SlotUtils.MAIN_END; slot++) {
            if (mc.player.getInventory().getStack(slot).isEmpty()) return false;
        }

        return true;
    }

    private int findSellableStack(int targetSlot) {
        if (isSellWhitelisted(mc.player.getInventory().getStack(targetSlot))) {
            return targetSlot;
        }

        for (int slot = SlotUtils.HOTBAR_START; slot <= SlotUtils.MAIN_END; slot++) {
            if (slot != targetSlot && isSellWhitelisted(mc.player.getInventory().getStack(slot))) {
                return slot;
            }
        }

        return -1;
    }

    private boolean isSellWhitelisted(ItemStack stack) {
        return !stack.isEmpty() && sellWhitelist.get().contains(stack.getItem());
    }

    private String normalizedCommand(String value) {
        value = value.trim();
        if (value.isEmpty()) return null;
        return value.startsWith("/") ? value : "/" + value;
    }

    private boolean isPlayerInventoryOpen() {
        return mc.player.currentScreenHandler == mc.player.playerScreenHandler;
    }

    private void restoreSellSlot() {
        if (sellPreviousSelectedSlot == -1) return;

        if (restoreSelectedSlot.get() && mc.player != null && mc.interactionManager != null) {
            InvUtils.swap(sellPreviousSelectedSlot, false);
        }
        sellPreviousSelectedSlot = -1;
    }

    private void resetRepairState() {
        repairCooldown = 0;
        pendingRepair = RepairType.None;
        repairPreviousSelectedSlot = -1;
        pendingArmorSlotId = -1;
        pendingArmorHotbarSlot = -1;
        displacedArmorHotbarItemSlot = -1;
        repairSession = false;
        resumeBaritone = false;
        warnedInvalidRepairCommand = false;
        warnedNoArmorStorage = false;
    }

    private enum RepairType {
        None,
        Tool,
        Armor
    }
}
