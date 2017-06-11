package com.github.ustc_zzzz.authlibloginhelper.asm;

import com.github.ustc_zzzz.authlibloginhelper.event.AuthlibLoginStartEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author ustc_zzzz
 */
@SideOnly(Side.CLIENT)
public class AuthlibLoginHelperHooks
{
    private static String ipConnecting = "127.0.0.1";
    private static int portConnecting = 25565;

    public static void connectCallback(String ip, int port)
    {
        ipConnecting = ip;
        portConnecting = port;
    }

    public static void loginCallback()
    {
        MinecraftForge.EVENT_BUS.post(new AuthlibLoginStartEvent(ipConnecting, portConnecting));
    }
}
