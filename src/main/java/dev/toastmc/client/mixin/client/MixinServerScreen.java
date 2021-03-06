package dev.toastmc.client.mixin.client;

import dev.toastmc.client.gui.auth.AuthScreen;
import dev.toastmc.client.util.LoginUtil;
import dev.toastmc.client.util.Status;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.options.ServerList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MixinServerScreen extends Screen {

    @Shadow protected  MultiplayerServerListWidget serverListWidget;
    @Shadow private ServerList serverList;

    private static Status status = Status.UNKNOWN;
    private TexturedButtonWidget authButton;
    private ButtonWidget toastmcButton;
    private ButtonWidget testServerButton;

    private final ServerInfo toastdev = new ServerInfo(" TOASTMC.DEV", "toastmc.dev", false);
    private final ServerInfo testServer = new ServerInfo(" TEST SERVER", "139.99.208.240:42069", false);

    protected MixinServerScreen(Text text_1) {
        super(text_1);
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        toastmcButton = new ButtonWidget(30, 6, 100, 20, new LiteralText("PLAY TOASTMC.DEV"), button -> {
            serverList.loadFile();
            serverList.add(toastdev);
            serverList.saveFile();
            serverListWidget.setSelected(null);
            serverListWidget.setServers(this.serverList);
        });
        this.addButton(toastmcButton);

        testServerButton = new ButtonWidget(134, 6, 100, 20, new LiteralText("TEST SERVER"), button -> {
            serverList.loadFile();
            serverList.add(testServer);
            serverList.saveFile();
            serverListWidget.setSelected(null);
            serverListWidget.setServers(this.serverList);
        });
//        this.addButton(testServerButton);

        authButton = new TexturedButtonWidget(6,
                6,
                20,
                20,
                0,
                146,
                20,
                new Identifier("minecraft:textures/gui/widgets.png"),
                256,
                256,
                button -> this.client.openScreen(new AuthScreen(this)),
                new TranslatableText("Authenticate"));
        this.addButton(authButton);

        // Fetch current session status
        MixinServerScreen.status = Status.UNKNOWN;
        LoginUtil.INSTANCE.getStatus().thenAccept(status -> MixinServerScreen.status = status);
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        // Draw status text/icon on button
        assert this.client != null;
        drawCenteredString(matrices, this.client.textRenderer, Formatting.BOLD + status.toString(), authButton.x + authButton.getWidth(), authButton.y - 1, status.getColor());
    }
}
