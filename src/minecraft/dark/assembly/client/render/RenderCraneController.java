package dark.assembly.client.render;

import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslated;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeDirection;
import dark.assembly.client.model.ModelCraneController;
import dark.assembly.common.AssemblyLine;
import dark.assembly.common.machine.crane.CraneHelper;
import dark.assembly.common.machine.crane.TileEntityCraneController;

public class RenderCraneController extends RenderImprintable
{
    public static final String TEXTURE = "crane_controller_off.png";
    public static final String TEXTURE_VALID = "crane_controller_on.png";
    public static final ModelCraneController MODEL = new ModelCraneController();

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float f)
    {
        if (tileEntity != null && tileEntity instanceof TileEntityCraneController)
        {
            ResourceLocation name = new ResourceLocation(AssemblyLine.instance.DOMAIN, AssemblyLine.MODEL_DIRECTORY + (((TileEntityCraneController) tileEntity).isCraneValid() ? TEXTURE_VALID : TEXTURE));
            func_110628_a(name);
            ForgeDirection front = ForgeDirection.getOrientation(tileEntity.worldObj.getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord));
            ForgeDirection right = CraneHelper.rotateClockwise(front);
            float angle = 0f;
            switch (front)
            {
                case NORTH:
                {
                    angle = 90f;
                    break;
                }
                case SOUTH:
                {
                    angle = 270f;
                    break;
                }
                case EAST:
                {
                    angle = 180f;
                    break;
                }
            }
            int tX, tY, tZ;
            tX = tileEntity.xCoord;
            tY = tileEntity.yCoord;
            tZ = tileEntity.zCoord;
            boolean connectFront = CraneHelper.canFrameConnectTo(tileEntity, tX + front.offsetX, tY, tZ + front.offsetZ, front.getOpposite());
            boolean connectRight = CraneHelper.canFrameConnectTo(tileEntity, tX + right.offsetX, tY, tZ + right.offsetZ, right.getOpposite());
            glPushMatrix();
            glTranslated(x + 0.5, y + 1.5, z + 0.5);
            glRotatef(180f, 0f, 0f, 1f);
            glRotatef(angle, 0f, 1f, 0f);
            glEnable(GL_LIGHTING);
            MODEL.render(0.0625f, connectRight, connectFront);
            glPopMatrix();
        }
    }

}