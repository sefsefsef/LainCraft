package lain.mods.skinmanager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lain.mods.laincraft.Plugin;
import lain.mods.laincraft.asm.SharedConstants;
import lain.mods.laincraft.event.ClientPlayerUpdateSkinEvent;
import lain.mods.laincraft.util.configuration.Config;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;

public class SkinManager extends Plugin implements IPacketHandler, IConnectionHandler
{

    @Config.SingleComment("by setting this to true, this server will force connected LainCraft clients to use specific servers defined in this file.")
    @Config.Property(defaultValue = "false")
    private static boolean forceSkins;
    @Config.SingleComment("official server: http://skins.minecraft.net/MinecraftSkins/%s.png" + "\n" + "old official server: http://s3.amazonaws.com/MinecraftSkins/%s.png" + "\n" + "McMySkin: http://mcmyskin.com/skins.php?name=%s.png" + "\n" + "SkinMe: http://sm.skinme.co/get.php?type=skin&file=%s.png")
    @Config.Property(defaultValue = "http://skins.minecraft.net/MinecraftSkins/%s.png")
    private static String SkinServer;
    @Config.SingleComment("official server: http://skins.minecraft.net/MinecraftCloaks/%s.png" + "\n" + "old official server: http://s3.amazonaws.com/MinecraftCloaks/%s.png" + "\n" + "McMySkin: http://mcmyskin.com/skins.php?cape&name=%s.png" + "\n" + "SkinMe: http://sm.skinme.co/get.php?type=cloak&file=%s.png")
    @Config.Property(defaultValue = "http://skins.minecraft.net/MinecraftCloaks/%s.png")
    private static String CloakServer;

    private HashMap<String, String> textures = new HashMap<String, String>();

    private Side side;
    private Config config;

    @Override
    public void clientLoggedIn(NetHandler paramNetHandler, INetworkManager paramINetworkManager, Packet1Login paramPacket1Login)
    {
        config.load();
        config.save();
        paramINetworkManager.addToSendQueue(new Packet250CustomPayload("LC|SM", ("SkinServer").getBytes()));
        paramINetworkManager.addToSendQueue(new Packet250CustomPayload("LC|SM", ("CloakServer").getBytes()));
    }

    @Override
    public void connectionClosed(INetworkManager paramINetworkManager)
    {
    }

    @Override
    public void connectionOpened(NetHandler paramNetHandler, MinecraftServer paramMinecraftServer, INetworkManager paramINetworkManager)
    {
    }

    @Override
    public void connectionOpened(NetHandler paramNetHandler, String paramString, int paramInt, INetworkManager paramINetworkManager)
    {
    }

    @Override
    public String connectionReceived(NetLoginHandler paramNetLoginHandler, INetworkManager paramINetworkManager)
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "SkinManager";
    }

    @Subscribe
    public void init(FMLPreInitializationEvent event)
    {
        side = event.getSide();
        if (side.isClient())
        {
            try
            {
                readFromZip(SharedConstants.getCoreJarFile(), "lain/mods/laincraft/skins/");
                File skinsDir = new File(event.getModConfigurationDirectory().getParentFile(), "skins");
                if (!skinsDir.isDirectory())
                    skinsDir.delete();
                if (!skinsDir.exists())
                    skinsDir.mkdirs();
                if (skinsDir.isDirectory())
                    for (File f : skinsDir.listFiles())
                        if (f.getName().toLowerCase().endsWith(".zip"))
                            if (f.isFile() && readFromZip(f, null))
                                SharedConstants.getActualClassLoader().addURL(f.toURI().toURL());
                System.out.println("LainCraft: loaded " + textures.size() + " offline skins");
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    @Subscribe
    public void load(FMLInitializationEvent event)
    {
        NetworkRegistry.instance().registerChannel(this, "LC|SM");
        if (side.isClient())
        {
            MinecraftForge.EVENT_BUS.register(this);
            NetworkRegistry.instance().registerConnectionHandler(this);
        }
    }

    @Override
    public void onDisable()
    {
    }

    @Override
    public void onEnable()
    {
        config = getConfig();
        config.register(SkinManager.class, null);
        config.load();
        config.save();
    }

    @Override
    public void onPacketData(INetworkManager paramINetworkManager, Packet250CustomPayload paramPacket250CustomPayload, Player paramPlayer)
    {
        String var1[] = new String(paramPacket250CustomPayload.data).split("\u0000");
        if (paramPlayer instanceof EntityPlayerMP)
        {
            if ("SkinServer".equals(var1[0]) && forceSkins)
                paramINetworkManager.addToSendQueue(new Packet250CustomPayload("LC|SM", ("SkinServer\u0000" + SkinServer).getBytes()));
            else if ("CloakServer".equals(var1[0]) && forceSkins)
                paramINetworkManager.addToSendQueue(new Packet250CustomPayload("LC|SM", ("CloakServer\u0000" + CloakServer).getBytes()));
        }
        else
        {
            if ("SkinServer".equals(var1[0]))
            {
                SkinServer = var1[1];
                System.out.println("LainCraft: SkinServer has been set to [" + SkinServer + "]");
            }
            else if ("CloakServer".equals(var1[0]))
            {
                CloakServer = var1[1];
                System.out.println("LainCraft: CloakServer has been set to [" + CloakServer + "]");
            }
        }
    }

    @ForgeSubscribe
    public void onUpdateSkins(ClientPlayerUpdateSkinEvent event)
    {
        String username = StringUtils.stripControlCodes(event.player.username);
        String name = username.toLowerCase();
        if (textures.containsKey(name))
        {
            String t = textures.get(name);
            if (t != null)
                event.texture = t;
        }
        event.skinUrl = String.format(SkinServer, username);
        event.cloakUrl = String.format(CloakServer, username);
    }

    @Override
    public void playerLoggedIn(Player paramPlayer, NetHandler paramNetHandler, INetworkManager paramINetworkManager)
    {
    }

    private boolean readFromZip(File file, String prefix) throws IOException
    {
        boolean found = false;
        ZipFile zip = null;
        try
        {
            zip = new ZipFile(file);
            for (ZipEntry entry : Collections.list(zip.entries()))
            {
                String name = entry.getName().toLowerCase();
                if (!name.endsWith(".png"))
                    continue;
                if (prefix == null)
                {
                    int i = name.lastIndexOf("/");
                    if (i != -1)
                        name = name.substring(i + 1);
                }
                else
                {
                    if (!name.startsWith(prefix))
                        continue;
                    name = name.substring(prefix.length());
                    if (name.indexOf("/") != -1)
                        continue;
                }
                name = name.substring(0, name.lastIndexOf("."));
                if (!textures.containsKey(name))
                {
                    textures.put(name, "/" + entry.getName());
                    found = true;
                }
            }
        }
        finally
        {
            if (zip != null)
                zip.close();
        }
        return found;
    }

}
