package com.github.ustc_zzzz.authlibloginhelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
        private static final Map<String, AuthlibLoginHelper.Data> RESULTS = ImmutableMap.of(
                I18n.format("gui.authlibloginhelper.forget"), new AuthlibLoginHelper.Data("", "", "", ""),
                I18n.format("gui.authlibloginhelper.skip"), new AuthlibLoginHelper.Data(""));

        private static final List<String> SKIP_CHOICES = ImmutableList.of(
                I18n.format("gui.authlibloginhelper.skip"),
                I18n.format("gui.authlibloginhelper.forget"));
        private static final List<String> LOGIN_CHOICES = ImmutableList.of(
                I18n.format("gui.authlibloginhelper.login"),
                I18n.format("gui.authlibloginhelper.forget"),
                I18n.format("gui.authlibloginhelper.skip"));

        private static final String SKIP_COMMENT = I18n.format("gui.authlibloginhelper.skip.tooltip");
        private static final String LOGIN_COMMENT = I18n.format("gui.authlibloginhelper.login.tooltip");

        private static final Map<String, Property> PROPERTIES = new HashMap<>();

        public Config(GuiScreen parent)
        {
            super(parent, getConfigElements(), "authlibloginhelper", false, false, getConfigTitle());
        }

        public static void onConfigApplied()
        {
            AuthlibLoginHelper instance = AuthlibLoginHelper.getInstance();
            for (Property property : PROPERTIES.values())
            {
                if (property.hasChanged())
                {
                    String address = property.getName();
                    AuthlibLoginHelper.Data data = RESULTS.get(property.getString());
                    if (data != null)
                    {
                        instance.saveAccount(address, data);
                    }
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
                boolean skip = data.userid.isEmpty() && !data.accessToken.isPresent();

                String[] choices = skip ? SKIP_CHOICES.toArray(new String[0]) : LOGIN_CHOICES.toArray(new String[0]);
                Property property = new Property(name, choices[0], Property.Type.STRING, choices);
                property.setComment(skip ? SKIP_COMMENT : LOGIN_COMMENT);

                builder.add(new ConfigElement(property));
                PROPERTIES.put(name, property);
            }
            return builder.build();
        }
    }
}
