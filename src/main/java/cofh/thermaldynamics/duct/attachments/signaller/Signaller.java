package cofh.thermaldynamics.duct.attachments.signaller;

import cofh.core.network.PacketCoFHBase;
import cofh.core.render.RenderUtils;
import cofh.lib.util.helpers.ServerHelper;
import cofh.repack.codechicken.lib.vec.Cuboid6;
import cofh.repack.codechicken.lib.vec.Translation;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.AttachmentRegistry;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.render.RenderDuct;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Facing;

public class Signaller extends Attachment {
    public Signaller(TileTDBase tile, byte side) {
        super(tile, side);
    }

    public Signaller(TileTDBase tile, byte side, int type) {
        super(tile, side);
        this.type = type;
    }

    int type = 0;

    @Override
    public String getName() {
        return "item.thermaldynamics.signaller.name";
    }

    @Override
    public int getId() {
        return AttachmentRegistry.SIGNALLER;
    }

    @Override
    public Cuboid6 getCuboid() {
        return TileTDBase.subSelection[side].copy();
    }

    @Override
    public TileTDBase.NeighborTypes getNeighborType() {
        return TileTDBase.NeighborTypes.DUCT_ATTACHMENT;
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean render(int pass, RenderBlocks renderBlocks) {
        if (pass == 1) {
            return false;
        }
        Translation trans = RenderUtils.getRenderVector(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5).translation();
        RenderDuct.modelConnection[1 + type][side].render(trans,
                RenderUtils.getIconTransformation(RenderDuct.signalTexture));
        return true;
    }

    @Override
    public ItemStack getPickBlock() {
        return new ItemStack(ThermalDynamics.itemSignaller);
    }

    @Override
    public List<ItemStack> getDrops() {

        LinkedList<ItemStack> drops = new LinkedList<ItemStack>();
        drops.add(getPickBlock());
        return drops;
    }


    boolean powered;

    @Override
    public void onNeighborChange() {
        super.onNeighborChange();

        if (type == 0) {
            powered = tile.world().getIndirectPowerOutput(
                    tile.x() + Facing.offsetsXForSide[side],
                    tile.y() + Facing.offsetsYForSide[side],
                    tile.z() + Facing.offsetsZForSide[side],
                    side
            );

            if (tile.myGrid != null) {
                tile.myGrid.signallumUpToDate = false;
            }
        }

        if (tile.myGrid != null) {
            tile.myGrid.signallumUpToDate = false;
        }
    }

    public boolean isInput() {
        return type == 0;
    }

    public boolean isOutput() {
        return type == 1;
    }

    public boolean isPowered() {
        if (type == 1 && tile.myGrid != null)
            return tile.myGrid.signallumPowered;
        return powered;
    }

    @Override
    public int getRSOutput() {
        return isOutput() && isPowered() ? 15 : 0;
    }

    public void setPowered(boolean powered) {


        if(this.powered != powered) {
            this.powered = powered;

            tile.world().notifyBlockOfNeighborChange(
                    tile.xCoord + Facing.offsetsXForSide[side],
                    tile.yCoord + Facing.offsetsYForSide[side],
                    tile.zCoord + Facing.offsetsZForSide[side],
                    tile.getBlockType());
        }

    }

    @Override
    public void checkSignal() {

        MultiBlockGrid grid = tile.myGrid;
        if (grid == null) return;
        setPowered(!powered);
    }

    @Override
    public boolean respondsToSignallum() {
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setByte("type", (byte) type);

    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        type = tag.getByte("type");
    }

    @Override
    public void addDescriptionToPacket(PacketCoFHBase packet) {

        packet.addByte(type);
    }

    @Override
    public void getDescriptionFromPacket(PacketCoFHBase packet) {

        type = packet.getByte();
    }

    @Override
    public boolean openGui(EntityPlayer player) {

        if(ServerHelper.isClientWorld(tile.world()))
            return true;

        type = type ^ 1;
        if (tile.myGrid != null)
            tile.myGrid.resetSignallers();
        tile.world().notifyBlocksOfNeighborChange(tile.xCoord, tile.yCoord, tile.zCoord, tile.getBlockType());
        onNeighborChange();
        tile.world().markBlockForUpdate(tile.x(), tile.y(), tile.z());
        return true;
    }

    @Override
    public boolean shouldRSConnect() {
        return true;
    }


    public boolean canAddToTile(TileTDBase tileMultiBlock) {

        return tileMultiBlock.getDuctType().frameType != 2;
    }

}
