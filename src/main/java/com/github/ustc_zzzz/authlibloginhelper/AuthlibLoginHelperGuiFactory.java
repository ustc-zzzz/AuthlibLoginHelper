package com.github.ustc_zzzz.authlibloginhelper;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ustc_zzzz
 */
@SideOnly(Side.CLIENT)
public class AuthlibLoginHelperGuiFactory implements IModGuiFactory
{
    @Override
    public void initialize(Minecraft mc)
    {
        // do nothing
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass()
    {
        return Config.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
    {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element)
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static class Config extends GuiConfig
    {
        private static final List<String> VALID_VALUES = ImmutableList.of(
                I18n.format("gui.authlibloginhelper.skip"),
                I18n.format("gui.authlibloginhelper.login"));

        private static final List<String> VALUE_COMMENTS = ImmutableList.of(
                I18n.format("gui.authlibloginhelper.skip.tooltip"),
                I18n.format("gui.authlibloginhelper.login.tooltip"));

        private static final Map<String, Property> PROPERTIES = new HashMap<>();

        public Config(GuiScreen parent)
        {
            super(parent, getConfigElements(), "authlibloginhelper", false, false, getConfigTitle());
        }

        public static void onConfigApplied()
        {
            for (Property property : PROPERTIES.values())
            {
                String address = property.getName();
                AuthlibLoginHelper.Data data = null;
                if (property.hasChanged())
                {
                    switch (VALID_VALUES.indexOf(property.getString()))
                    {
                    case 0:
                        data = new AuthlibLoginHelper.Data("");
                        break;
                    case 1:
                        data = new AuthlibLoginHelper.Data("", "", "", "");
                        break;
                    default:
                        // what happened?
                    }
                }
                if (data != null)
                {
                    AuthlibLoginHelper.getInstance().saveAccount(address, data);
                }
            }
        }

        private static String getConfigTitle()
        {
            return "AuthlibLoginHelper";
        }

        private static List<IConfigElement> getConfigElements()
        {
            PROPERTIES.clear();
            ImmutableList.Builder<IConfigElement> builder = ImmutableList.builder();
            Map<String, AuthlibLoginHelper.Data> accounts = AuthlibLoginHelper.getInstance().listAccounts();
            for (Map.Entry<String, AuthlibLoginHelper.Data> entry : accounts.entrySet())
            {
                String name = entry.getKey();
                AuthlibLoginHelper.Data data = entry.getValue();
                String[] validValues = VALID_VALUES.toArray(new String[0]);
                int index = data.userid.isEmpty() && !data.accessToken.isPresent() ? 0 : 1;

                Property property = new Property(name, VALID_VALUES.get(index), Property.Type.STRING, validValues);
                property.setComment(I18n.format(VALUE_COMMENTS.get(index)));
                builder.add(new ConfigElement(property));
                PROPERTIES.put(name, property);
            }
            return builder.build();
        }
    }
}
