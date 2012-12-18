package assemblyline.common.machine;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ContainerSorter extends Container
{
	private TileEntityRejector tileEntity;

	public ContainerSorter(InventoryPlayer par1InventoryPlayer, TileEntityRejector tileEntity)
	{
		this.tileEntity = tileEntity;
		for (int i = 0; i < 4; i++)
		{
			this.addSlotToContainer(new Slot(tileEntity, 0 + i, 33 + i * 18, 34));
		}
		int var3;

		for (var3 = 0; var3 < 3; ++var3)
		{
			for (int var4 = 0; var4 < 9; ++var4)
			{
				this.addSlotToContainer(new Slot(par1InventoryPlayer, var4 + var3 * 9 + 9, 8 + var4 * 18, 84 + var3 * 18));
			}
		}

		for (var3 = 0; var3 < 9; ++var3)
		{
			this.addSlotToContainer(new Slot(par1InventoryPlayer, var3, 8 + var3 * 18, 142));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer par1EntityPlayer)
	{
		return this.tileEntity.isUseableByPlayer(par1EntityPlayer);
	}

	/**
	 * Called to transfer a stack from one inventory to the other eg. when shift clicking.
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par1)
	{
		ItemStack itemStack3 = null;
		Slot itemStack = (Slot) this.inventorySlots.get(par1);

		if (itemStack != null && itemStack.getHasStack())
		{
			ItemStack itemStack2 = itemStack.getStack();
			itemStack3 = itemStack2.copy();

			if (par1 != 0)
			{
				if (itemStack2.itemID == Item.coal.shiftedIndex)
				{
					if (!this.mergeItemStack(itemStack2, 0, 1, false)) { return null; }
				}
				else if (par1 >= 30 && par1 < 37 && !this.mergeItemStack(itemStack2, 3, 30, false)) { return null; }
			}
			else if (!this.mergeItemStack(itemStack2, 3, 37, false)) { return null; }

			if (itemStack2.stackSize == 0)
			{
				itemStack.putStack((ItemStack) null);
			}
			else
			{
				itemStack.onSlotChanged();
			}

			if (itemStack2.stackSize == itemStack3.stackSize) { return null; }

			itemStack.onPickupFromSlot(par1EntityPlayer, itemStack2);
		}

		return itemStack3;
	}
}