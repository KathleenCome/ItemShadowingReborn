package com.etprovince.itemshadowingreborn.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
import java.util.Set;

@Mixin(ScreenHandler.class)
public abstract class ItemMixin {
    @Shadow private int quickCraftStage;

    @Shadow public static int unpackQuickCraftStage(int quickCraftData) {
        return quickCraftData & 3;
    }

    @Shadow protected abstract void endQuickCraft();

    @Shadow public abstract ItemStack getCursorStack();

    @Shadow private int quickCraftButton;

    @Shadow public static int unpackQuickCraftButton(int quickCraftData) {
        return quickCraftData >> 2 & 3;
    }

    @Shadow public static boolean shouldQuickCraftContinue(int stage, PlayerEntity player) {
        if (stage == 0) {
            return true;
        } else if (stage == 1) {
            return true;
        } else {
            return stage == 2 && player.getAbilities().creativeMode;
        }
    }

    @Shadow @Final private Set<Slot> quickCraftSlots;

    @Shadow @Final public DefaultedList<Slot> slots;

    @Shadow public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
        boolean bl = slot == null || !slot.hasStack();
        if (!bl && ItemStack.canCombine(stack, slot.getStack())) {
            return slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount();
        } else {
            return bl;
        }
    }

    @Shadow public abstract void setCursorStack(ItemStack stack);

    @Shadow public abstract boolean canInsertIntoSlot(Slot slot);

    @Shadow public static void calculateStackSize(Set<Slot> slots, int mode, ItemStack stack, int stackSize) {
        switch (mode) {
            case 0:
                stack.setCount(MathHelper.floor((float)stack.getCount() / (float)slots.size()));
                break;
            case 1:
                stack.setCount(1);
                break;
            case 2:
                stack.setCount(stack.getItem().getMaxCount());
        }

        stack.increment(stackSize);
    }

    @Shadow public abstract ItemStack transferSlot(PlayerEntity player, int index);

    @Shadow protected abstract StackReference getCursorStackReference();

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
                    this.quickCraftStage = 1;
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
                    k = this.getCursorStack().getCount();
                    Iterator<Slot> var9 = this.quickCraftSlots.iterator();

                    label305:
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
                                            break label305;
                                        }

                                        slot2 = (Slot)var9.next();
                                        itemStack3 = this.getCursorStack();
                                    } while(slot2 == null);
                                } while(!canInsertItemIntoSlot(slot2, itemStack3, true));
                            } while(!slot2.canInsert(itemStack3));
                        } while(this.quickCraftButton != 2 && itemStack3.getCount() < this.quickCraftSlots.size());

                        if (this.canInsertIntoSlot(slot2)) {
                            ItemStack itemStack4 = itemStack2.copy();
                            int l = slot2.hasStack() ? slot2.getStack().getCount() : 0;
                            calculateStackSize(this.quickCraftSlots, this.quickCraftButton, itemStack4, l);
                            int m = Math.min(itemStack4.getMaxCount(), slot2.getMaxItemCount(itemStack4));
                            if (itemStack4.getCount() > m) {
                                itemStack4.setCount(m);
                            }

                            k -= itemStack4.getCount() - l;
                            slot2.setStack(itemStack4);
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
            int n;
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

                    for(itemStack = this.transferSlot(player, slotIndex); !itemStack.isEmpty() && ItemStack.areItemsEqualIgnoreDamage(slot.getStack(), itemStack); itemStack = this.transferSlot(player, slotIndex)) {
                    }
                } else {
                    if (slotIndex < 0) {
                        return;
                    }

                    slot = (Slot)this.slots.get(slotIndex);
                    itemStack = slot.getStack();
                    ItemStack itemStack5 = this.getCursorStack();
                    player.onPickupSlotClick(itemStack5, slot.getStack(), clickType);
                    if (!itemStack5.onStackClicked(slot, clickType, player) && !itemStack.onClicked(itemStack5, slot, clickType, player, this.getCursorStackReference())) {
                        if (itemStack.isEmpty()) {
                            if (!itemStack5.isEmpty()) {
                                n = clickType == ClickType.LEFT ? itemStack5.getCount() : 1;
                                this.setCursorStack(slot.insertStack(itemStack5, n));
                            }
                        } else if (slot.canTakeItems(player)) {
                            if (itemStack5.isEmpty()) {
                                n = clickType == ClickType.LEFT ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                                Optional<ItemStack> optional = slot.tryTakeStackRange(n, Integer.MAX_VALUE, player);
                                optional.ifPresent((stack) -> {
                                    this.setCursorStack(stack);
                                    slot.onTakeItem(player, stack);
                                });
                            } else if (slot.canInsert(itemStack5)) {
                                if (ItemStack.canCombine(itemStack, itemStack5)) {
                                    n = clickType == ClickType.LEFT ? itemStack5.getCount() : 1;
                                    this.setCursorStack(slot.insertStack(itemStack5, n));
                                } else if (itemStack5.getCount() <= slot.getMaxItemCount(itemStack5)) {
                                    this.setCursorStack(itemStack);
                                    slot.setStack(itemStack5);
                                }
                            } else if (ItemStack.canCombine(itemStack, itemStack5)) {
                                Optional<ItemStack> optional2 = slot.tryTakeStackRange(itemStack.getCount(), itemStack5.getMaxCount() - itemStack5.getCount(), player);
                                optional2.ifPresent((stack) -> {
                                    itemStack5.increment(stack.getCount());
                                    slot.onTakeItem(player, stack);
                                });
                            }
                        }
                    }

                    slot.markDirty();
                }
            } else {
                Slot slot3;
                int o;
                if (actionType == SlotActionType.SWAP) {
                    slot3 = (Slot)this.slots.get(slotIndex);
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
                                o = slot3.getMaxItemCount(itemStack2);
                                if (itemStack2.getCount() > o) {
                                    slot3.setStack(itemStack2.split(o));
                                } else {
                                    slot3.setStack(itemStack2);
                                    playerInventory.setStack(button, ItemStack.EMPTY);
                                }
                            }
                        } else if (slot3.canTakeItems(player) && slot3.canInsert(itemStack2)) {
                            o = slot3.getMaxItemCount(itemStack2);
                            if (itemStack2.getCount() > o) {
                                slot3.setStack(itemStack2.split(o));
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
                        itemStack2 = slot3.getStack().copy();
                        itemStack2.setCount(itemStack2.getMaxCount());
                        this.setCursorStack(itemStack2);
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
                        o = button == 0 ? 1 : -1;

                        for(n = 0; n < 2; ++n) {
                            for(int p = k; p >= 0 && p < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxCount(); p += o) {
                                Slot slot4 = (Slot)this.slots.get(p);
                                if (slot4.hasStack() && canInsertItemIntoSlot(slot4, itemStack2, true) && slot4.canTakeItems(player) && this.canInsertIntoSlot(slot4)) {
                                    ItemStack itemStack6 = slot4.getStack();
                                    if (n != 0 || itemStack6.getCount() != itemStack6.getMaxCount()) {
                                        ItemStack itemStack7 = slot4.takeStackRange(itemStack6.getCount(), itemStack2.getMaxCount() - itemStack2.getCount(), player);
                                        itemStack2.increment(itemStack7.getCount());
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
