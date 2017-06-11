package com.github.ustc_zzzz.authlibloginhelper.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * @author ustc_zzzz
 */
@IFMLLoadingPlugin.MCVersion("1.10.2")
@IFMLLoadingPlugin.Name("AuthlibLoginHelperCore")
@IFMLLoadingPlugin.TransformerExclusions("com.github.ustc_zzzz.authlibloginhelper.asm.")
public class AuthlibLoginHelperCore implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[0];
    }

    @Override
    public String getModContainerClass()
    {
        return "com.github.ustc_zzzz.authlibloginhelper.asm.AuthlibLoginHelperModContainer";
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        // do nothing
    }

    @Override
    public String getAccessTransformerClass()
    {
        return "com.github.ustc_zzzz.authlibloginhelper.asm.AuthlibLoginHelperTransformer";
    }

}
