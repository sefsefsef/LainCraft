package lain.mods.bilicraftcomments.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import cpw.mods.fml.client.FMLClientHandler;

public class Comment
{

    public static final List<CommentSlot> slots;
    public static final int numSlots = 20;

    static
    {
        slots = new ArrayList(numSlots);
        for (int i = 0; i < numSlots; i++)
            slots.add(new CommentSlot(i));
    }

    public static Minecraft client = null;
    public static FontRenderer renderer = null;
    public static ScaledResolution res = null;
    public static int width = 320;
    public static int height = 240;

    public static void postRender()
    {
    }

    public static void preRender()
    {
        client = FMLClientHandler.instance().getClient();
        renderer = client.fontRenderer;
        res = new ScaledResolution(client.gameSettings, client.displayWidth, client.displayHeight);
        width = res.getScaledWidth();
        height = res.getScaledHeight();
    }

    public final int mode;
    public final String text;
    public final int lifespan;

    public final long ticksCreated;

    public int slot = -1;
    public int expandedLife = 0;

    public int x;
    public int y;
    public int color;
    public boolean shadow;

    public static String debug = "";

    public Comment(int mode, String text, int lifespan, long ticks)
    {
        this.mode = mode;
        this.text = text;
        this.lifespan = lifespan;
        this.ticksCreated = ticks;
    }

    public void assignSlot(long ticks)
    {
        if (slot == -1)
        {
            CommentSlot s = null;
            boolean f = false;
            switch (mode)
            {
                case 0: // Normal
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    f = true;
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    break;
                case 1: // Top
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    f = true;
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    break;
                case 2: // Bottom
                    for (int i = numSlots - 1; i >= 0; i--)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    f = true;
                    for (int i = numSlots - 1; i >= 0; i--)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    break;
                case 3: // Backward
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    f = true;
                    for (int i = 0; i < numSlots; i++)
                    {
                        s = slots.get(i);
                        if (s != null && !s.isOccupied(ticks, mode, f))
                            break;
                    }
                    break;
            }
            if (s != null && s.set(ticks, mode, f))
                slot = s.slotNumber;
        }
        if (slot == -1)
            expandedLife = Math.max(0, (int) (ticks - ticksCreated));
    }

    public void draw()
    {
        renderer.drawString(debug, 0, 0, 0xFFFFFF, true);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, 0.0F);
        renderer.drawString(text, 0, 0, color, shadow);
        GL11.glPopMatrix();
    }

    public boolean isDead(long ticks)
    {
        if (slot == -1)
            return false;
        return lifespan > 0 && (ticks >= ticksCreated + lifespan + expandedLife);
    }

    public void onAdd()
    {
        assignSlot(ticksCreated);
    }

    public void onRemove()
    {
        slot = -1;
    }

    public int textWidth()
    {
        return renderer.getStringWidth(text);
    }

    public void update(long ticks, float partialTicks)
    {
        if (slot == -1)
            assignSlot(ticks);
        if (slot != -1)
        {
            float f1 = Math.min(((float) (ticks - ticksCreated) + partialTicks) / (float) (lifespan + expandedLife), 1.0F);
            float f2;
            int w = textWidth();
            switch (mode)
            {
                case 0:
                    f2 = 1F - f1;
                    x = (int) (f2 * (width + w)) - w;
                    break;
                case 1:
                case 2:
                    x = (width - w) / 2;
                    break;
                case 3:
                    f2 = f1;
                    x = (int) (f2 * (width + w)) - w;
                    break;
            }
            y = (slot + 1) * (renderer.FONT_HEIGHT + 1);
            color = 0xFFFFFF;
            shadow = true;
            debug = String.format("%d %d %f %d %d %d", width, height, f1, x, y, slot);
        }
    }

}