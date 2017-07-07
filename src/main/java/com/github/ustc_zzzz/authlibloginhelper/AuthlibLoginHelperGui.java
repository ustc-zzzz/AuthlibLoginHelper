package com.github.ustc_zzzz.authlibloginhelper;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.SettableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Tuple;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * @author ustc_zzzz
 */
public class AuthlibLoginHelperGui extends GuiScreen
{
    private final Minecraft mc;
    private final GuiConnecting parent;
    private final String previousUsername;
    private final SettableFuture<Tuple<String, String>> futureUsernamePassword;

    private GuiTextField usernameField;
    private GuiTextField passwordField;
    private GuiButton noButton;
    private GuiButton okButton;
    private GuiButton skipButton;

    public AuthlibLoginHelperGui(GuiConnecting parent, Minecraft minecraft, String username)
    {
        this.mc = minecraft;
        this.parent = parent;
        this.previousUsername = username;
        this.futureUsernamePassword = SettableFuture.create();
    }

    @Override
    public void initGui()
    {
        int offsetX = this.width / 2, offsetY = this.height / 2;
        String noButtonText = I18n.format("gui.authlibloginhelper.no");
        String skipButtonText = I18n.format("gui.authlibloginhelper.skip");
        String okButtonText = I18n.format("gui.authlibloginhelper.ok");
        this.noButton = new GuiButton(-1, offsetX + 51, offsetY + 30, 50, 20, noButtonText);
        this.skipButton = new GuiButton(0, offsetX - 9, offsetY + 30, 50, 20, skipButtonText);
        this.okButton = new GuiButton(1, offsetX - 69, offsetY + 30, 50, 20, okButtonText);
        this.usernameField = new GuiTextField(2, this.mc.fontRendererObj, offsetX - 100, offsetY - 50, 200, 20);
        this.passwordField = new GuiTextField(3, this.mc.fontRendererObj, offsetX - 100, offsetY - 10, 200, 20)
        {
            @Override
            public synchronized void drawTextBox()
            {
                String text = this.getText();
                this.setText(Strings.repeat("*", text.length()));
                super.drawTextBox();
                this.setText(text);
            }
        };

        this.buttonList.clear();
        this.buttonList.add(noButton);
        this.okButton.enabled = false;
        this.buttonList.add(okButton);
        this.buttonList.add(skipButton);
        this.usernameField.setMaxStringLength(256);
        this.usernameField.setText(this.previousUsername);
        this.usernameField.setFocused(this.previousUsername.isEmpty());
        this.passwordField.setMaxStringLength(4096); // is it enough?
        this.passwordField.setFocused(!this.previousUsername.isEmpty());
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        this.passwordField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen()
    {
        if (this.usernameField != null && this.passwordField != null)
        {
            this.usernameField.updateCursorCounter();
            this.passwordField.updateCursorCounter();
            this.okButton.enabled = !this.usernameField.getText().isEmpty() && !this.passwordField.getText().isEmpty();
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        this.usernameField.textboxKeyTyped(typedChar, keyCode);
        this.passwordField.textboxKeyTyped(typedChar, keyCode);
        if (this.okButton.enabled && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER))
        {
            this.actionPerformed(this.okButton);
        }
        else if (keyCode == Keyboard.KEY_TAB)
        {
            boolean isUsernameFieldFocused = this.usernameField.isFocused();
            boolean isPasswordFieldFocused = this.passwordField.isFocused();
            this.usernameField.setFocused(isPasswordFieldFocused);
            this.passwordField.setFocused(isUsernameFieldFocused);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == this.okButton.id)
        {
            this.mc.displayGuiScreen(this.parent);
            String username = this.usernameField.getText();
            String password = this.passwordField.getText();
            this.futureUsernamePassword.set(new Tuple<>(username, password));
        }
        else if (button.id == this.skipButton.id)
        {
            this.mc.displayGuiScreen(this.parent);
            this.futureUsernamePassword.set(new Tuple<>("", ""));
        }
        else if (button.id == this.noButton.id)
        {
            this.futureUsernamePassword.set(null);
            this.parent.actionPerformed(this.skipButton); // button.id == 0
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        if (this.usernameField != null && this.passwordField != null)
        {
            this.usernameField.drawTextBox();
            this.passwordField.drawTextBox();
            int offsetX = this.width / 2, offsetY = this.height / 2;
            String usernameText = I18n.format("gui.authlibloginhelper.username");
            String passwordText = I18n.format("gui.authlibloginhelper.password");
            this.drawString(this.fontRendererObj, usernameText, offsetX - 100, offsetY - 62, 0x00A0A0A0);
            this.drawString(this.fontRendererObj, passwordText, offsetX - 100, offsetY - 22, 0x00A0A0A0);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    public Future<Tuple<String, String>> getUsernamePassword()
    {
        return this.futureUsernamePassword;
    }
}
