package dark.assembly.common.armbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.grid.IElectricityNetwork;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.TranslationHelper;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import dark.assembly.api.IArmbot;
import dark.assembly.common.AssemblyLine;
import dark.assembly.common.armbot.command.Command;
import dark.assembly.common.armbot.command.CommandDrop;
import dark.assembly.common.armbot.command.CommandFire;
import dark.assembly.common.armbot.command.CommandGrab;
import dark.assembly.common.armbot.command.CommandManager;
import dark.assembly.common.armbot.command.CommandReturn;
import dark.assembly.common.armbot.command.CommandRotateBy;
import dark.assembly.common.armbot.command.CommandRotateTo;
import dark.assembly.common.armbot.command.CommandUse;
import dark.assembly.common.machine.TileEntityAssembly;
import dark.assembly.common.machine.encoder.ItemDisk;
import dark.core.DarkMain;
import dark.core.blocks.IMultiBlock;
import dark.core.helpers.ItemFindingHelper;

public class TileEntityArmbot extends TileEntityAssembly implements IMultiBlock, IInventory, IPacketReceiver, IArmbot, IPeripheral
{
    private final CommandManager commandManager = new CommandManager();
    /** The items this container contains. */
    protected ItemStack disk = null;
    private int computersAttached = 0;
    private List<IComputerAccess> connectedComputers = new ArrayList<IComputerAccess>();
    /** The rotation of the arms. In Degrees. */
    public float rotationPitch = 0;
    public float rotationYaw = 0;
    public float renderPitch = 0;
    public float renderYaw = 0;
    public final float ROTATION_SPEED = 2.0f;

    private String displayText = "";

    public boolean isProvidingPower = false;

    /** An entity that the Armbot is grabbed onto. Entity Items are held separately. */
    private final List<Entity> grabbedEntities = new ArrayList<Entity>();
    private final List<ItemStack> grabbedItems = new ArrayList<ItemStack>();

    /** Client Side Object Storage */
    public EntityItem renderEntityItem = null;

    public TileEntityArmbot()
    {
        super(20);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initiate()
    {
        super.initiate();
        if (!this.commandManager.hasTasks())
        {
            this.onInventoryChanged();
        }
    }

    @Override
    public void onUpdate()
    {
        Vector3 handPosition = this.getHandPosition();

        for (Entity entity : this.grabbedEntities)
        {
            if (entity != null)
            {
                entity.setPosition(handPosition.x, handPosition.y, handPosition.z);
                entity.motionX = 0;
                entity.motionY = 0;
                entity.motionZ = 0;

                if (entity instanceof EntityItem)
                {
                    ((EntityItem) entity).delayBeforeCanPickup = 20;
                    ((EntityItem) entity).age = 0;
                }
            }
        }

        if (this.isRunning())
        {
            if (!this.worldObj.isRemote)
            {
                if (this.disk == null && this.computersAttached == 0)
                {
                    this.commandManager.clear();

                    if (this.grabbedEntities.size() > 0 || this.grabbedItems.size() > 0)
                    {
                        this.addCommand(CommandDrop.class);
                    }
                    else
                    {
                        if (!this.commandManager.hasTasks())
                        {
                            if (Math.abs(this.rotationYaw - CommandReturn.IDLE_ROTATION_YAW) > 0.01 || Math.abs(this.rotationPitch - CommandReturn.IDLE_ROTATION_PITCH) > 0.01)
                            {
                                this.addCommand(CommandReturn.class);
                            }
                        }
                    }

                    this.commandManager.setCurrentTask(0);
                }
            }
            if (!this.worldObj.isRemote)
            {
                this.commandManager.onUpdate();
            }
        }
        else
        {
        }

        if (!this.worldObj.isRemote)
        {
            if (!this.commandManager.hasTasks())
            {
                this.displayText = "";
            }
            else
            {
                try
                {
                    Command curCommand = this.commandManager.getCommands().get(this.commandManager.getCurrentTask());
                    if (curCommand != null)
                    {
                        this.displayText = curCommand.toString();
                    }
                }
                catch (Exception ex)
                {
                }
            }
        }

        // System.out.println("Ren: " + this.renderYaw + "; Rot: " +
        // this.rotationYaw);
        if (Math.abs(this.renderYaw - this.rotationYaw) > 0.001f)
        {
            float speedYaw;
            if (this.renderYaw > this.rotationYaw)
            {
                if (Math.abs(this.renderYaw - this.rotationYaw) >= 180)
                    speedYaw = this.ROTATION_SPEED;
                else
                    speedYaw = -this.ROTATION_SPEED;
            }
            else
            {
                if (Math.abs(this.renderYaw - this.rotationYaw) >= 180)
                    speedYaw = -this.ROTATION_SPEED;
                else
                    speedYaw = this.ROTATION_SPEED;
            }

            this.renderYaw += speedYaw;

            // keep it within 0 - 360 degrees so ROTATE commands work properly
            while (this.renderYaw < 0)
                this.renderYaw += 360;
            while (this.renderYaw > 360)
                this.renderYaw -= 360;

            if (this.ticks % 5 == 0 && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            {
                // sound is 0.25 seconds long (20 ticks/second)
                this.worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, "mods.assemblyline.conveyor", 0.4f, 1.7f, true);
            }

            if (Math.abs(this.renderYaw - this.rotationYaw) < this.ROTATION_SPEED + 0.1f)
            {
                this.renderYaw = this.rotationYaw;
            }

            for (Entity e : (ArrayList<Entity>) this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord + 2, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1)))
            {
                e.rotationYaw = this.renderYaw;
            }
        }

        if (Math.abs(this.renderPitch - this.rotationPitch) > 0.001f)
        {
            float speedPitch;
            if (this.renderPitch > this.rotationPitch)
            {
                speedPitch = -this.ROTATION_SPEED;
            }
            else
            {
                speedPitch = this.ROTATION_SPEED;
            }

            this.renderPitch += speedPitch;

            while (this.renderPitch < 0)
                this.renderPitch += 60;
            while (this.renderPitch > 60)
                this.renderPitch -= 60;

            if (this.ticks % 4 == 0 && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
                this.worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, "mods.assemblyline.conveyor", 2f, 2.5f, true);

            if (Math.abs(this.renderPitch - this.rotationPitch) < this.ROTATION_SPEED + 0.1f)
            {
                this.renderPitch = this.rotationPitch;
            }

            for (Entity e : (ArrayList<Entity>) this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord + 2, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1)))
            {
                e.rotationPitch = this.renderPitch;
            }
        }

        while (this.rotationYaw < 0)
            this.rotationYaw += 360;
        while (this.rotationYaw > 360)
            this.rotationYaw -= 360;
        while (this.rotationPitch < 0)
            this.rotationPitch += 60;
        while (this.rotationPitch > 60)
            this.rotationPitch -= 60;

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && this.ticks % 20 == 0)
        {
            PacketManager.sendPacketToClients(this.getDescriptionPacket(), this.worldObj, new Vector3(this), 50);
        }
    }

    public Command getCurrentCommand()
    {
        if (this.commandManager.hasTasks() && this.commandManager.getCurrentTask() >= 0 && this.commandManager.getCurrentTask() < this.commandManager.getCommands().size())
            return this.commandManager.getCommands().get(this.commandManager.getCurrentTask());
        return null;
    }

    /** @return The current hand position of the armbot. */
    public Vector3 getHandPosition()
    {
        Vector3 position = new Vector3(this);
        position.add(0.5);
        position.add(this.getDeltaHandPosition());
        return position;
    }

    public Vector3 getDeltaHandPosition()
    {
        // The distance of the position relative to the main position.
        double distance = 1f;
        Vector3 delta = new Vector3();
        // The delta Y of the hand.
        delta.y = Math.sin(Math.toRadians(this.renderPitch)) * distance * 2;
        // The horizontal delta of the hand.
        double dH = Math.cos(Math.toRadians(this.renderPitch)) * distance;
        // The delta X and Z.
        delta.x = Math.sin(Math.toRadians(-this.renderYaw)) * dH;
        delta.z = Math.cos(Math.toRadians(-this.renderYaw)) * dH;
        return delta;
    }

    /** Data */
    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return PacketManager.getPacket(AssemblyLine.CHANNEL, this, AssemblyTilePacket.NBT, nbt);
    }

    /** Inventory */
    @Override
    public int getSizeInventory()
    {
        return 1;
    }

    @Override
    public String getInvName()
    {
        return TranslationHelper.getLocal("tile.armbot.name");
    }

    /** Inventory functions. */
    @Override
    public ItemStack getStackInSlot(int par1)
    {
        return this.disk;
    }

    @Override
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.disk != null)
        {
            ItemStack var3;

            if (this.disk.stackSize <= par2)
            {
                var3 = this.disk;
                this.disk = null;
                return var3;
            }
            else
            {
                var3 = this.disk.splitStack(par2);

                if (this.disk.stackSize == 0)
                {
                    this.disk = null;
                }

                return var3;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (this.disk != null)
        {
            ItemStack var2 = this.disk;
            this.disk = null;
            return var2;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        this.disk = par2ItemStack;
        this.onInventoryChanged();
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
    {
        return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openChest()
    {
    }

    @Override
    public void closeChest()
    {
    }

    public String getCommandDisplayText()
    {
        return this.displayText;
    }

    /** NBT Data */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        NBTTagCompound diskNBT = nbt.getCompoundTag("disk");

        if (diskNBT != null)
        {
            this.disk = ItemStack.loadItemStackFromNBT(diskNBT);
        }
        else
        {
            this.disk = null;
        }

        this.rotationYaw = nbt.getFloat("yaw");
        this.rotationPitch = nbt.getFloat("pitch");

        if (this.worldObj != null)
        {
            if (this.worldObj.isRemote)
            {
                this.displayText = nbt.getString("cmdText");
            }
        }

        this.commandManager.setCurrentTask(nbt.getInteger("curTask"));

        NBTTagList entities = nbt.getTagList("entities");
        this.grabbedEntities.clear();
        for (int i = 0; i < entities.tagCount(); i++)
        {
            NBTTagCompound entityTag = (NBTTagCompound) entities.tagAt(i);
            if (entityTag != null)
            {
                Entity entity = EntityList.createEntityFromNBT(entityTag, worldObj);
                this.grabbedEntities.add(entity);
            }
        }

        NBTTagList items = nbt.getTagList("items");
        this.grabbedItems.clear();
        for (int i = 0; i < items.tagCount(); i++)
        {
            NBTTagCompound itemTag = (NBTTagCompound) items.tagAt(i);
            if (itemTag != null)
            {
                ItemStack item = ItemStack.loadItemStackFromNBT(itemTag);
                this.grabbedItems.add(item);
            }
        }
    }

    /** Writes a tile entity to NBT. */
    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        NBTTagCompound diskNBT = new NBTTagCompound();

        if (this.disk != null)
        {
            this.disk.writeToNBT(diskNBT);
        }

        nbt.setTag("disk", diskNBT);
        nbt.setFloat("yaw", this.rotationYaw);
        nbt.setFloat("pitch", this.rotationPitch);

        nbt.setString("cmdText", this.displayText);

        nbt.setInteger("curTask", this.commandManager.getCurrentTask());

        NBTTagList entities = new NBTTagList();

        for (Entity entity : grabbedEntities)
        {
            if (entity != null)
            {
                NBTTagCompound entityNBT = new NBTTagCompound();
                entity.writeToNBT(entityNBT);
                entity.addEntityID(entityNBT);
                entities.appendTag(entityNBT);
            }
        }

        nbt.setTag("entities", entities);

        NBTTagList items = new NBTTagList();

        for (ItemStack itemStack : grabbedItems)
        {
            if (itemStack != null)
            {
                NBTTagCompound entityNBT = new NBTTagCompound();
                itemStack.writeToNBT(entityNBT);
                items.appendTag(entityNBT);
            }
        }

        nbt.setTag("items", items);
    }

    @Override
    public boolean onActivated(EntityPlayer player)
    {
        ItemStack containingStack = this.getStackInSlot(0);

        if (containingStack != null)
        {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
            {
                EntityItem dropStack = new EntityItem(this.worldObj, player.posX, player.posY, player.posZ, containingStack);
                dropStack.delayBeforeCanPickup = 0;
                this.worldObj.spawnEntityInWorld(dropStack);
            }

            this.setInventorySlotContents(0, null);
            return true;
        }
        else
        {
            if (player.getCurrentEquippedItem() != null)
            {
                if (player.getCurrentEquippedItem().getItem() instanceof ItemDisk)
                {
                    this.setInventorySlotContents(0, player.getCurrentEquippedItem());
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onInventoryChanged()
    {
        this.commandManager.clear();

        if (this.disk != null)
        {
            List<String> commands = ItemDisk.getCommands(this.disk);

            for (String commandString : commands)
            {
                String commandName = commandString.split(" ")[0];

                Class<? extends Command> command = Command.getCommand(commandName);

                if (command != null)
                {
                    List<String> commandParameters = new ArrayList<String>();

                    for (String param : commandString.split(" "))
                    {
                        if (!param.equals(commandName))
                        {
                            commandParameters.add(param);
                        }
                    }

                    this.addCommand(command, commandParameters.toArray(new String[0]));
                }
            }
        }
        else
        {
            this.addCommand(Command.getCommand("DROP"));
            this.addCommand(Command.getCommand("RETURN"));
        }
    }

    public void addCommand(Class<? extends Command> command)
    {
        this.commandManager.addCommand(this, command);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
        {
            PacketManager.sendPacketToClients(this.getDescriptionPacket(), this.worldObj, new Vector3(this), 50);
        }
    }

    public void addCommand(Class<? extends Command> command, String[] parameters)
    {
        this.commandManager.addCommand(this, command, parameters);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
        {
            PacketManager.sendPacketToClients(this.getDescriptionPacket(), this.worldObj, new Vector3(this), 50);
        }
    }

    @Override
    public void onCreate(Vector3 placedPosition)
    {
        if (DarkMain.blockMulti != null)
        {
            DarkMain.blockMulti.makeFakeBlock(this.worldObj, Vector3.add(placedPosition, new Vector3(0, 1, 0)), placedPosition);
        }
    }

    @Override
    public void onDestroy(TileEntity callingBlock)
    {
        this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, 0, 0, 3);
        this.worldObj.setBlock(this.xCoord, this.yCoord + 1, this.zCoord, 0, 0, 3);
    }

    @Override
    public String getType()
    {
        return "ArmBot";
    }

    @Override
    public String[] getMethodNames()
    {
        return new String[] { "rotateBy", "rotateTo", "grab", "drop", "reset", "isWorking", "touchingEntity", "use", "fire", "return", "clear", "isHolding" };
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
    {
        switch (method)
        {
            case 0: // rotateBy: rotates by a certain amount
            {
                if (arguments.length > 0)
                {
                    try
                    // try to cast to Float
                    {
                        double yaw = (Double) arguments[0];
                        double pitch = (Double) arguments[1];
                        this.addCommand(CommandRotateBy.class, new String[] { Double.toString(yaw), Double.toString(pitch) });
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        throw new IllegalArgumentException("expected number");
                    }
                }
                else
                {
                    throw new IllegalArgumentException("expected number");
                }
                break;
            }
            case 1:
            {
                // rotateTo: rotates to a specific rotation
                if (arguments.length > 0)
                {
                    try

                    {// try to cast to Float
                        double yaw = (Double) arguments[0];
                        double pitch = (Double) arguments[1];
                        this.addCommand(CommandRotateTo.class, new String[] { Double.toString(yaw), Double.toString(pitch) });
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        throw new IllegalArgumentException("expected number");
                    }
                }
                else
                {
                    throw new IllegalArgumentException("expected number");
                }
                break;
            }
            case 2:
            {
                // grab: grabs an item
                this.addCommand(CommandGrab.class);
                break;
            }
            case 3:
            {
                // drop: drops an item
                this.addCommand(CommandDrop.class);
                break;
            }
            case 4:
            {
                // reset: equivalent to calling .clear() then .return()
                this.commandManager.clear();
                this.addCommand(CommandReturn.class);
                break;
            }
            case 5:
            {
                // isWorking: returns whether or not the ArmBot is executing
                // commands
                return new Object[] { this.commandManager.hasTasks() };
            }
            case 6:
            {
                // touchingEntity: returns whether or not the ArmBot is touching an
                // entity it is
                // able to pick up
                Vector3 serachPosition = this.getHandPosition();
                List<Entity> found = this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(serachPosition.x - 0.5f, serachPosition.y - 0.5f, serachPosition.z - 0.5f, serachPosition.x + 0.5f, serachPosition.y + 0.5f, serachPosition.z + 0.5f));

                if (found != null && found.size() > 0)
                {
                    for (int i = 0; i < found.size(); i++)
                    {
                        if (found.get(i) != null && !(found.get(i) instanceof EntityPlayer) && found.get(i).ridingEntity == null)
                        {
                            return new Object[] { true };
                        }
                    }
                }

                return new Object[] { false };
            }
            case 7:
            {
                if (arguments.length > 0)
                {
                    try
                    {
                        // try to cast to Float
                        int times = (Integer) arguments[0];
                        this.addCommand(CommandUse.class, new String[] { Integer.toString(times) });
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        throw new IllegalArgumentException("expected number");
                    }
                }
                else
                {
                    this.addCommand(CommandUse.class);
                }
                break;
            }
            case 8: // fire: think "flying pig"
            {
                if (arguments.length > 0)
                {
                    try
                    {
                        // try to cast to Float
                        float strength = (float) ((double) ((Double) arguments[0]));
                        this.addCommand(CommandFire.class, new String[] { Float.toString(strength) });
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        throw new IllegalArgumentException("expected number");
                    }
                }
                else
                {
                    this.addCommand(CommandFire.class);
                }
                break;
            }
            case 9:
            {
                // return: returns to home position
                this.addCommand(CommandReturn.class);
                break;
            }
            case 10:
            {
                // clear: clears commands
                this.commandManager.clear();
                break;
            }
            case 11:
            {
                // isHolding: returns whether or not it is holding something
                return new Object[] { this.grabbedEntities.size() > 0 };
            }
        }
        return null;
    }

    @Override
    public boolean canAttachToSide(int side)
    {
        return side != ForgeDirection.UP.ordinal();
    }

    @Override
    public void attach(IComputerAccess computer)
    {
        computersAttached++;
        synchronized (connectedComputers)
        {
            connectedComputers.add(computer);
        }
    }

    @Override
    public void detach(IComputerAccess computer)
    {
        computersAttached--;
        synchronized (connectedComputers)
        {
            connectedComputers.remove(computer);
        }
    }

    @Override
    public List<Entity> getGrabbedEntities()
    {
        return this.grabbedEntities;
    }

    @Override
    public List<ItemStack> getGrabbedItems()
    {
        return this.grabbedItems;
    }

    @Override
    public void grabEntity(Entity entity)
    {
        if (entity instanceof EntityItem)
        {
            this.grabItem(((EntityItem) entity).getEntityItem());
            entity.setDead();
        }
        else
        {
            this.grabbedEntities.add(entity);
        }
    }

    @Override
    public void grabItem(ItemStack itemStack)
    {
        this.grabbedItems.add(itemStack);
    }

    @Override
    public void drop(Object object)
    {
        if (object instanceof Entity)
        {
            this.grabbedEntities.remove((Entity) object);
        }
        if (object instanceof ItemStack)
        {
            Vector3 handPosition = this.getHandPosition();
            ItemFindingHelper.dropItemStackExact(worldObj, handPosition.x, handPosition.y, handPosition.z, (ItemStack) object);
            this.grabbedItems.remove((ItemStack) object);
        }
        if (object instanceof String)
        {
            String string = ((String) object).toLowerCase();
            if (string.equalsIgnoreCase("all"))
            {
                Vector3 handPosition = this.getHandPosition();
                Iterator<ItemStack> it = this.grabbedItems.iterator();

                while (it.hasNext())
                {
                    ItemFindingHelper.dropItemStackExact(worldObj, handPosition.x, handPosition.y, handPosition.z, it.next());
                }

                this.grabbedEntities.clear();
                this.grabbedItems.clear();
            }
        }
    }

    /** called by the block when another checks it too see if it is providing power to a direction */
    public boolean isProvidingPowerSide(ForgeDirection dir)
    {
        return this.isProvidingPower && dir.getOpposite() == this.getFacingDirectionFromAngle();
    }

    /** gets the facing direction using the yaw angle */
    public ForgeDirection getFacingDirectionFromAngle()
    {
        float angle = MathHelper.wrapAngleTo180_float(this.rotationYaw);
        if (angle >= -45 && angle <= 45)
        {
            return ForgeDirection.SOUTH;
        }
        else if (angle >= 45 && angle <= 135)
        {

            return ForgeDirection.WEST;
        }
        else if (angle >= 135 && angle <= -135)
        {

            return ForgeDirection.NORTH;
        }
        else
        {
            return ForgeDirection.EAST;
        }
    }

    @Override
    public boolean canConnect(ForgeDirection direction)
    {
        return direction == ForgeDirection.DOWN;
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return false;
    }

    @Override
    public double getWattLoad()
    {
        if (this.getCurrentCommand() != null)
        {
            return 2;
        }
        return .1;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public float getCurrentCapacity()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setNetwork(IElectricityNetwork network)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh()
    {
        // TODO Auto-generated method stub

    }
}
