package com.etprovince.itemshadowingreborn.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@Mixin(ScreenHandler.class)
public abstract class ItemMixin {


    @Shadow private int quickCraftStage;

    @Shadow public static int unpackQuickCraftStage(int quickCraftData) {
        return quickCraftData & 3;
    }

    @Shadow protected abstract void endQuickCraft();

    @Shadow public static int unpackQuickCraftButton(int quickCraftData) {
        return quickCraftData >> 2 & 3;
    };

    @Shadow public static boolean shouldQuickCraftContinue(int stage, PlayerEntity player) {
        if (stage == 0) {
            return true;
        } else if (stage == 1) {
            return true;
        } else {
            return stage == 2 && player.getAbilities().creativeMode;
        }
    }

    @Shadow private int quickCraftButton;

    @Shadow @Final private Set<Slot> quickCraftSlots;

    @Shadow @Final public DefaultedList<Slot> slots;

    @Shadow public abstract ItemStack getCursorStack();

    @Shadow public abstract void setCursorStack(ItemStack stack);

    @Shadow public abstract boolean canInsertIntoSlot(Slot slot);

    @Shadow public abstract boolean canInsertIntoSlot(ItemStack stack, Slot slot);

    @Shadow public static int calculateStackSize(Set<Slot> slots, int mode, ItemStack stack) {
        int var10000;
        switch (mode) {
            case 0:
                var10000 = MathHelper.floor((float)stack.getCount() / (float)slots.size());
                break;
            case 1:
                var10000 = 1;
                break;
            case 2:
                var10000 = stack.getItem().getMaxCount();
                break;
            default:
                var10000 = stack.getCount();
        }

        return var10000;
    }

    @Shadow public abstract ItemStack quickMove(PlayerEntity player, int slot);

    @Shadow protected abstract StackReference getCursorStackReference();

    @Shadow public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
        boolean bl = slot == null || !slot.hasStack();
        if (!bl && ItemStack.canCombine(stack, slot.getStack())) {
            return slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount();
        } else {
            return bl;
        }
    }
    @Shadow private boolean handleSlotClick(PlayerEntity player, ClickType clickType, Slot slot, ItemStack stack, ItemStack cursorStack) {
        FeatureSet featureSet = player.getWorld().getEnabledFeatures();
        if (cursorStack.isItemEnabled(featureSet) && cursorStack.onStackClicked(slot, clickType, player)) {
            return true;
        } else {
            return stack.isItemEnabled(featureSet) && stack.onClicked(cursorStack, slot, clickType, player, this.getCursorStackReference());
        }
    }


    @Shadow @Final private DefaultedList<ItemStack> trackedStacks;

    @Shadow public abstract OptionalInt getSlotIndex(Inventory inventory, int index);

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        PlayerInventory playerInventory = player.getInventory();
        Slot slot;
        ItemStack itemStack;
        ItemStack itemStack2;
        int j;
        int k;
        if (actionType == SlotActionType.QUICK_CRAFT) {
            int i = this.quickCraftStage;
            this.quickCraftStage = unpackQuickCraftStage(button);
            if ((i != 1 || this.quickCraftStage != 2) && i != this.quickCraftStage) {
                this.endQuickCraft();
            } else if (this.getCursorStack().isEmpty()) {
                this.endQuickCraft();
            } else if (this.quickCraftStage == 0) {
                this.quickCraftButton = unpackQuickCraftButton(button);
                if (shouldQuickCraftContinue(this.quickCraftButton, player)) {
                    this.quickCraftStage= 1;
                    this.quickCraftSlots.clear();
                } else {
                    this.endQuickCraft();
                }
            } else if (this.quickCraftStage == 1) {
                slot = (Slot)this.slots.get(slotIndex);
                itemStack = this.getCursorStack();
                if (canInsertItemIntoSlot(slot, itemStack, true) && slot.canInsert(itemStack) && (this.quickCraftButton == 2 || itemStack.getCount() > this.quickCraftSlots.size()) && this.canInsertIntoSlot(slot)) {
                    this.quickCraftSlots.add(slot);
                }
            } else if (this.quickCraftStage == 2) {
                if (!this.quickCraftSlots.isEmpty()) {
                    if (this.quickCraftSlots.size() == 1) {
                        j = ((Slot)this.quickCraftSlots.iterator().next()).id;
                        this.endQuickCraft();
                        this.internalOnSlotClick(j, this.quickCraftButton, SlotActionType.PICKUP, player);
                        return;
                    }

                    itemStack2 = this.getCursorStack().copy();
                    if (itemStack2.isEmpty()) {
                        this.endQuickCraft();
                        return;
                    }

                    k = this.getCursorStack().getCount();
                    Iterator<Slot> var9 = this.quickCraftSlots.iterator();

                    label303:
                    while(true) {
                        Slot slot2;
                        ItemStack itemStack3;
                        do {
                            do {
                                do {
                                    do {
                                        if (!var9.hasNext()) {
                                            itemStack2.setCount(k);
                                            this.setCursorStack(itemStack2);
                                            break label303;
                                        }

                                        slot2 = (Slot)var9.next();
                                        itemStack3 = this.getCursorStack();
                                    } while(slot2 == null);
                                } while(!canInsertItemIntoSlot(slot2, itemStack3, true));
                            } while(!slot2.canInsert(itemStack3));
                        } while(this.quickCraftButton != 2 && itemStack3.getCount() < this.quickCraftSlots.size());

                        if (this.canInsertIntoSlot(slot2)) {
                            int l = slot2.hasStack() ? slot2.getStack().getCount() : 0;
                            int m = Math.min(itemStack2.getMaxCount(), slot2.getMaxItemCount(itemStack2));
                            int n = Math.min(calculateStackSize(this.quickCraftSlots, this.quickCraftButton, itemStack2) + l, m);
                            k -= n - l;
                            slot2.setStack(itemStack2.copyWithCount(n));
                        }
                    }
                }

                this.endQuickCraft();
            } else {
                this.endQuickCraft();
            }
        } else if (this.quickCraftStage != 0) {
            this.endQuickCraft();
        } else {
            int o;
            if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) && (button == 0 || button == 1)) {
                ClickType clickType = button == 0 ? ClickType.LEFT : ClickType.RIGHT;
                if (slotIndex == -999) {
                    if (!this.getCursorStack().isEmpty()) {
                        if (clickType == ClickType.LEFT) {
                            player.dropItem(this.getCursorStack(), true);
                            this.setCursorStack(ItemStack.EMPTY);
                        } else {
                            player.dropItem(this.getCursorStack().split(1), true);
                        }
                    }
                } else if (actionType == SlotActionType.QUICK_MOVE) {
                    if (slotIndex < 0) {
                        return;
                    }

                    slot = (Slot)this.slots.get(slotIndex);
                    if (!slot.canTakeItems(player)) {
                        return;
                    }

                    for(itemStack = this.quickMove(player, slotIndex); !itemStack.isEmpty() && ItemStack.areItemsEqual(slot.getStack(), itemStack); itemStack = this.quickMove(player, slotIndex)) {
                    }
                } else {
                    if (slotIndex < 0) {
                        return;
                    }

                    slot = (Slot)this.slots.get(slotIndex);
                    itemStack = slot.getStack();
                    ItemStack itemStack4 = this.getCursorStack();
                    player.onPickupSlotClick(itemStack4, slot.getStack(), clickType);
                    if (!this.handleSlotClick(player, clickType, slot, itemStack, itemStack4)) {
                        if (itemStack.isEmpty()) {
                            if (!itemStack4.isEmpty()) {
                                o = clickType == ClickType.LEFT ? itemStack4.getCount() : 1;
                                this.setCursorStack(slot.insertStack(itemStack4, o));
                            }
                        } else if (slot.canTakeItems(player)) {
                            if (itemStack4.isEmpty()) {
                                o = clickType == ClickType.LEFT ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                                Optional<ItemStack> optional = slot.tryTakeStackRange(o, Integer.MAX_VALUE, player);
                                optional.ifPresent((stack) -> {
                                    this.setCursorStack(stack);
                                    slot.onTakeItem(player, stack);
                                });
                            } else if (slot.canInsert(itemStack4)) {
                                if (ItemStack.canCombine(itemStack, itemStack4)) {
                                    o = clickType == ClickType.LEFT ? itemStack4.getCount() : 1;
                                    this.setCursorStack(slot.insertStack(itemStack4, o));
                                } else if (itemStack4.getCount() <= slot.getMaxItemCount(itemStack4)) {
                                    this.setCursorStack(itemStack);
                                    slot.setStack(itemStack4);
                                }
                            } else if (ItemStack.canCombine(itemStack, itemStack4)) {
                                Optional<ItemStack> optional2 = slot.tryTakeStackRange(itemStack.getCount(), itemStack4.getMaxCount() - itemStack4.getCount(), player);
                                optional2.ifPresent((stack) -> {
                                    itemStack4.increment(stack.getCount());
                                    slot.onTakeItem(player, stack);
                                });
                            }
                        }
                    }

                    slot.markDirty();
                }
            } else {
                Slot slot3;
                int p;
                if (actionType == SlotActionType.SWAP) {
                    slot3 = (Slot) this.slots.get(slotIndex);
                    itemStack2 = playerInventory.getStack(button);
                    itemStack = slot3.getStack();
                    if (!itemStack2.isEmpty() || !itemStack.isEmpty()) {
                        if (itemStack2.isEmpty()) {
                            if (slot3.canTakeItems(player)) {
                                playerInventory.setStack(button, itemStack);
//                                slot3.onTake(itemStack.getCount());
                                slot3.setStack(ItemStack.EMPTY);
                                slot3.onTakeItem(player, itemStack);
                            }
                        } else if (itemStack.isEmpty()) {
                            if (slot3.canInsert(itemStack2)) {
                                p = slot3.getMaxItemCount(itemStack2);
                                if (itemStack2.getCount() > p) {
                                    slot3.setStack(itemStack2.split(p));
                                } else {
                                    slot3.setStack(itemStack2);
                                    playerInventory.setStack(button, ItemStack.EMPTY);
                                }
                            }
                        } else if (slot3.canTakeItems(player) && slot3.canInsert(itemStack2)) {
                            p = slot3.getMaxItemCount(itemStack2);
                            if (itemStack2.getCount() > p) {
                                slot3.setStack(itemStack2.split(p));
                                slot3.onTakeItem(player, itemStack);
                                if (!playerInventory.insertStack(itemStack)) {
                                    player.dropItem(itemStack, true);
                                }
                            } else {
                                slot3.setStack(itemStack2);
                                playerInventory.setStack(button, itemStack);
                                slot3.onTakeItem(player, itemStack);
                            }
                        }
                    }
                } else if (actionType == SlotActionType.CLONE && player.getAbilities().creativeMode && this.getCursorStack().isEmpty() && slotIndex >= 0) {
                    slot3 = (Slot)this.slots.get(slotIndex);
                    if (slot3.hasStack()) {
                        itemStack2 = slot3.getStack();
                        this.setCursorStack(itemStack2.copyWithCount(itemStack2.getMaxCount()));
                    }
                } else if (actionType == SlotActionType.THROW && this.getCursorStack().isEmpty() && slotIndex >= 0) {
                    slot3 = (Slot)this.slots.get(slotIndex);
                    j = button == 0 ? 1 : slot3.getStack().getCount();
                    itemStack = slot3.takeStackRange(j, Integer.MAX_VALUE, player);
                    player.dropItem(itemStack, true);
                } else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
                    slot3 = (Slot)this.slots.get(slotIndex);
                    itemStack2 = this.getCursorStack();
                    if (!itemStack2.isEmpty() && (!slot3.hasStack() || !slot3.canTakeItems(player))) {
                        k = button == 0 ? 0 : this.slots.size() - 1;
                        p = button == 0 ? 1 : -1;

                        for(o = 0; o < 2; ++o) {
                            for(int q = k; q >= 0 && q < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxCount(); q += p) {
                                Slot slot4 = (Slot) this.slots.get(q);
                                if (slot4.hasStack() && canInsertItemIntoSlot(slot4, itemStack2, true) && slot4.canTakeItems(player) && this.canInsertIntoSlot(itemStack2, slot4)) {
                                    ItemStack itemStack5 = slot4.getStack();
                                    if (o != 0 || itemStack5.getCount() != itemStack5.getMaxCount()) {
                                        ItemStack itemStack6 = slot4.takeStackRange(itemStack5.getCount(), itemStack2.getMaxCount() - itemStack2.getCount(), player);
                                        itemStack2.increment(itemStack6.getCount());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}