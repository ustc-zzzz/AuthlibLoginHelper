package com.github.ustc_zzzz.authlibloginhelper;

import com.github.ustc_zzzz.authlibloginhelper.event.AuthlibLoginStartEvent;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Session;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Proxy;

/**
 * @author ustc_zzzz
 */
@Mod(modid = "authlibloginhelper", name = "AuthlibLoginHelper", version = "@version@", clientSideOnly = true,
        dependencies = "required-after:authlibloginhelpercore", acceptedMinecraftVersions = "1.10.2")
public class AuthlibLoginHelper
{
    private File conf;
    private Logger logger;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private void saveAccount(String ip, int port, Data data)
    {
        try (FileReader reader = new FileReader(this.conf); FileWriter writer = new FileWriter(this.conf))
        {
            JsonObject jsonObject = this.gson.fromJson(reader, JsonObject.class);
            if (jsonObject == null)
            {
                jsonObject = new JsonObject();
            }
            JsonObject account = new JsonObject();
            account.addProperty("user", data.userid);
            account.addProperty("name", data.name.orNull());
            account.addProperty("uuid", data.uuid.orNull());
            account.addProperty("auth", data.accessToken.orNull());
            jsonObject.add(ip + ':' + port, account);
            this.gson.toJson(jsonObject, writer);
        }
        catch (IOException e)
        {
            String path = this.conf.getAbsolutePath();
            this.logger.info("AuthlibLoginHelper: Find error when saving account to " + path, e);
        }
    }

    private Data loadAccount(String ip, int port)
    {
        try (FileReader reader = new FileReader(this.conf))
        {
            JsonObject jsonObject = this.gson.fromJson(reader, JsonObject.class);
            if (jsonObject != null && jsonObject.has(ip + ':' + port))
            {
                JsonObject account = jsonObject.getAsJsonObject(ip + ':' + port);
                String userid = account.has("user") ? account.get("user").getAsString() : "";
                if (account.has("name") && account.has("uuid") && account.has("auth"))
                {
                    String name = account.get("name").getAsString();
                    String uuid = account.get("uuid").getAsString();
                    String accessToken = account.get("auth").getAsString();
                    return new Data(userid, name, uuid, accessToken);
                }
                else
                {
                    return new Data(userid);
                }
            }
            else
            {
                return new Data("", "", "", "");
            }
        }
        catch (IOException e)
        {
            String path = this.conf.getAbsolutePath();
            this.logger.info("AuthlibLoginHelper: Find error when loading account to " + path, e);
            return new Data("", "", "", "");
        }
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event)
    {
        try
        {
            this.logger = event.getModLog();
            this.conf = new File(Launch.minecraftHome, "authlibloginhelper-accounts.json");
            if (this.conf.createNewFile())
            {
                String path = this.conf.getAbsolutePath();
                this.logger.info("AuthlibLoginHelper: Successfully created " + path);
            }
            MinecraftForge.EVENT_BUS.register(this);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onLogin(AuthlibLoginStartEvent event)
    {
        Data oldData = this.loadAccount(event.ip, event.port);
        YggdrasilAuthenticationService s = new YggdrasilAuthenticationService(Proxy.NO_PROXY, "1");
        YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication) s.createUserAuthentication(Agent.MINECRAFT);

        Minecraft minecraft = Minecraft.getMinecraft();
        Session session = minecraft.getSession();
        String userid = oldData.userid;

        if (oldData.accessToken.isPresent())
        {
            try
            {
                if (!userid.isEmpty())
                {
                    auth.loadFromStorage(ImmutableMap.<String, Object>of("accessToken", oldData.accessToken.get()));
                    auth.logIn();
                    session.username = oldData.name.orNull();
                    session.playerID = oldData.uuid.orNull();
                    session.token = auth.getAuthenticatedToken();
                    Data newData = new Data(userid, session.username, session.playerID, session.token);
                    this.saveAccount(event.ip, event.port, newData);
                    return;
                }
            }
            catch (AuthenticationException e)
            {
                this.logger.info("AuthlibLoginHelper: Failed to login with access token, try to login with password");
                auth.logOut();
            }
        }
        else if (userid.isEmpty())
        {
            return;
        }

        GuiScreen currentScreen = minecraft.currentScreen;

        if (currentScreen instanceof GuiConnecting)
        {
            AuthlibLoginHelperGui gui = new AuthlibLoginHelperGui((GuiConnecting) currentScreen, minecraft, userid);
            minecraft.displayGuiScreen(gui);
            try
            {
                Tuple<String, String> result = gui.getUsernamePassword().get();
                if (result != null)
                {
                    userid = result.getFirst();
                    if (userid.isEmpty())
                    {
                        this.saveAccount(event.ip, event.port, new Data(userid));
                        return;
                    }
                    auth.setUsername(userid);
                    auth.setPassword(result.getSecond());
                    auth.logIn();
                    session.username = auth.getSelectedProfile().getName();
                    session.playerID = UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId());
                    session.token = auth.getAuthenticatedToken();
                    Data newData = new Data(userid, session.username, session.playerID, session.token);
                    this.saveAccount(event.ip, event.port, newData);
                }
            }
            catch (Exception e)
            {
                e = e instanceof AuthenticationException ? e : new AuthenticationException("Login failed", e);
                this.logger.error(e);
            }
        }
    }

    private static class Data
    {
        private final String userid;
        private final Optional<String> name;
        private final Optional<String> uuid;
        private final Optional<String> accessToken;

        private Data(String userid, String name, String uuid, String accessToken)
        {
            this.userid = userid;
            this.name = Optional.of(name);
            this.uuid = Optional.of(uuid);
            this.accessToken = Optional.of(accessToken);
        }

        private Data(String userid)
        {
            this.userid = userid;
            this.name = Optional.absent();
            this.uuid = Optional.absent();
            this.accessToken = Optional.absent();
        }
    }
}
