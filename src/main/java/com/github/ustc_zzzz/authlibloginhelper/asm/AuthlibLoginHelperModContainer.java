package com.github.ustc_zzzz.authlibloginhelper.asm;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.Collections;

/**
 * @author ustc_zzzz
 */
public class AuthlibLoginHelperModContainer extends DummyModContainer
{
    public AuthlibLoginHelperModContainer()
    {
        super(new ModMetadata());
        ModMetadata modMetadata = this.getMetadata();
        modMetadata.modId = "authlibloginhelpercore";
        modMetadata.name = "AuthlibLoginHelperCore";
        modMetadata.version = "@version@";
        modMetadata.authorList = Collections.singletonList("ustc_zzzz");
        modMetadata.description = "Inject a callback hook when connecting to server or LAN. ";
        modMetadata.credits = "Mojang AB, and the Forge and FML guys. ";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        return true;
    }
}
