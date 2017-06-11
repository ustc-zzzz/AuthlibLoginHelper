package com.github.ustc_zzzz.authlibloginhelper.event;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * @author ustc_zzzz
 */
public class AuthlibLoginStartEvent extends Event
{
    public final String ip;
    public final int port;

    public AuthlibLoginStartEvent(String ip, int port)
    {
        this.ip = ip;
        this.port = port;
    }
}
