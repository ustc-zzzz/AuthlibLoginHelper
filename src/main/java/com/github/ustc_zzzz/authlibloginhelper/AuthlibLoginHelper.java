package com.github.ustc_zzzz.authlibloginhelper;

import com.github.ustc_zzzz.authlibloginhelper.event.AuthlibLoginStartEvent;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author ustc_zzzz
 */
@Mod(modid = "authlibloginhelper", name = "AuthlibLoginHelper", version = "@version@", clientSideOnly = true,
        dependencies = "required-after:authlibloginhelpercore", acceptedMinecraftVersions = "1.10.2",
        guiFactory = "com.github.ustc_zzzz.authlibloginhelper.AuthlibLoginHelperGuiFactory")
public class AuthlibLoginHelper
{
    private static AuthlibLoginHelper instance;

    private File conf;
    private Logger logger;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String clientToken = Strings.repeat("f", 32); // magic number

    public Map<String, Data> listAccounts()
    {
        try (FileReader reader = new FileReader(this.conf))
        {
            ImmutableMap.Builder<String, Data> builder = ImmutableMap.builder();
            JsonElement jsonObject = this.gson.fromJson(reader, JsonElement.class);
            if (jsonObject == null || !jsonObject.isJsonObject())
            {
                jsonObject = new JsonObject();
            }
            for (Map.Entry<String, JsonElement> entry : jsonObject.getAsJsonObject().entrySet())
            {
                JsonObject account = entry.getValue().getAsJsonObject();
                String userid = account.has("user") ? account.get("user").getAsString() : "";
                if (!account.has("name") || !account.has("uuid") || !account.has("auth"))
                {
                    builder.put(entry.getKey(), new Data(userid));
                }
                else
                {
                    String name = account.get("name").getAsString();
                    String uuid = account.get("uuid").getAsString();
                    String accessToken = account.get("auth").getAsString();
                    builder.put(entry.getKey(), new Data(userid, name, uuid, accessToken));
                }
            }
            return builder.build();
        }
        catch (IOException e)
        {
            String path = this.conf.getAbsolutePath();
            this.logger.info("AuthlibLoginHelper: Find error when saving account to " + path, e);
            return ImmutableMap.of();
        }
    }

    public void saveAccount(String address, Data data)
    {
        try
        {
            JsonElement jsonObject;
            try (FileReader reader = new FileReader(this.conf))
            {
                jsonObject = this.gson.fromJson(reader, JsonElement.class);
                if (jsonObject == null || !jsonObject.isJsonObject())
                {
                    jsonObject = new JsonObject();
                }
                if (data.userid.isEmpty() && data.accessToken.isPresent())
                {
                    jsonObject.getAsJsonObject().remove(address);
                }
                else
                {
                    JsonObject account = new JsonObject();
                    account.addProperty("user", data.userid);
                    account.addProperty("name", data.name.orNull());
                    account.addProperty("uuid", data.uuid.orNull());
                    account.addProperty("auth", data.accessToken.orNull());
                    jsonObject.getAsJsonObject().add(address, account);
                }
            }
            try (FileWriter writer = new FileWriter(this.conf))
            {
                this.gson.toJson(jsonObject, writer);
            }
        }
        catch (IOException e)
        {
            String path = this.conf.getAbsolutePath();
            this.logger.info("AuthlibLoginHelper: Find error when saving account to " + path, e);
        }
    }

    public Data loadAccount(String address)
    {
        try (FileReader reader = new FileReader(this.conf))
        {
            JsonElement jsonObject = this.gson.fromJson(reader, JsonElement.class);
            if (jsonObject != null && jsonObject.isJsonObject() && jsonObject.getAsJsonObject().has(address))
            {
                JsonObject account = jsonObject.getAsJsonObject().getAsJsonObject(address);
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
            instance = this;
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
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if ("authlibloginhelper".equals(event.getModID()))
        {
            AuthlibLoginHelperGuiFactory.Config.onConfigApplied();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onLogin(AuthlibLoginStartEvent event)
    {
        Data oldData = this.loadAccount(event.ip + ':' + event.port);
        YggdrasilAuthenticationService s = new YggdrasilAuthenticationService(Proxy.NO_PROXY, this.clientToken);
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
                    auth.setUsername(userid);
                    auth.logIn();
                    session.username = oldData.name.orNull();
                    session.playerID = oldData.uuid.orNull();
                    session.token = auth.getAuthenticatedToken();
                    Data newData = new Data(userid, session.username, session.playerID, session.token);
                    this.saveAccount(event.ip + ':' + event.port, newData);
                    return;
                }
            }
            catch (AuthenticationException e)
            {
                this.logger.debug("AuthlibLoginHelper: Failed to login with access token", e);
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
                        this.saveAccount(event.ip + ':' + event.port, new Data(userid));
                        return;
                    }
                    auth.setUsername(userid);
                    auth.setPassword(result.getSecond());
                    auth.logIn();
                    session.username = auth.getSelectedProfile().getName();
                    session.playerID = UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId());
                    session.token = auth.getAuthenticatedToken();
                    Data newData = new Data(userid, session.username, session.playerID, session.token);
                    this.saveAccount(event.ip + ':' + event.port, newData);
                }
            }
            catch (Exception e)
            {
                e = e instanceof AuthenticationException ? e : new AuthenticationException("Login failed", e);
                this.logger.error("AuthlibLoginHelper: Failed to connect " + event.ip + ':' + event.port, e);
            }
        }
    }

    public static AuthlibLoginHelper getInstance()
    {
        return instance;
    }

    // (user, name, uuid, auth) match {
    //   case ("", _, _, null) => skip
    //   case ("", _, _, _) => login with username and password
    //   case (_, _, _, _) => login with access token, and with username and password if the token does not work
    // }
    public static class Data
    {
        public final String userid;
        public final Optional<String> name;
        public final Optional<String> uuid;
        public final Optional<String> accessToken;

        public Data(String userid, String name, String uuid, String accessToken)
        {
            this.userid = userid;
            this.name = Optional.of(name);
            this.uuid = Optional.of(uuid);
            this.accessToken = Optional.of(accessToken);
        }

        public Data(String userid)
        {
            this.userid = userid;
            this.name = Optional.absent();
            this.uuid = Optional.absent();
            this.accessToken = Optional.absent();
        }
    }
}
