package assemblyline.render;

import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

import org.lwjgl.opengl.GL11;

import assemblyline.AssemblyLine;
import assemblyline.machines.TileEntitySorter;
import assemblyline.model.ModelSorter;

public class RenderSorter extends TileEntitySpecialRenderer
{
	private ModelSorter model = new ModelSorter();

	public void renderAModelAt(TileEntitySorter tileEntity, double x, double y, double z, float f)
	{
		boolean fire = tileEntity.firePiston;
		int face = tileEntity.getDirection(tileEntity.worldObj.getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord));
		int pos = 0;
		if (fire)
		{
			pos = 8;
		}
		bindTextureByName(AssemblyLine.TEXTURE_PATH + "ejector.png");
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
		GL11.glScalef(1.0F, -1F, -1F);
		if (face == 2)
		{
			GL11.glRotatef(180f, 0f, 1f, 0f);
		}
		if (face == 3)
		{
			GL11.glRotatef(0f, 0f, 1f, 0f);
		}
		if (face == 4)
		{
			GL11.glRotatef(90f, 0f, 1f, 0f);
		}
		if (face == 5)
		{
			GL11.glRotatef(270f, 0f, 1f, 0f);
		}
		model.renderMain(0.0625F);
		model.renderPiston(0.0625F, pos);
		GL11.glPopMatrix();

	}

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double var2, double var4, double var6, float var8)
	{
		this.renderAModelAt((TileEntitySorter) tileEntity, var2, var4, var6, var8);
	}

}