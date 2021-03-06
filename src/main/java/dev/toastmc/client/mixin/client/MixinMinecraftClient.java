package dev.toastmc.client.mixin.client;

import dev.toastmc.client.ToastClient;
import dev.toastmc.client.event.ScreenEvent;
import dev.toastmc.client.event.TickEvent;
import dev.toastmc.client.gui.click.ClickGuiScreen;
import dev.toastmc.client.module.render.ClickGUI;
import dev.toastmc.client.module.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow
    public ClientWorld world;

    @Shadow public ClientPlayerEntity player;

    private static ClickGUI mod = (ClickGUI) ToastClient.Companion.getMODULE_MANAGER().getModuleByClass(ClickGUI.class);

    @Inject(method = "tick", at = @At(value = "INVOKE"), cancellable = true)
    public void tick(CallbackInfo info) {
        TickEvent.Client event;
        if (player != null && world != null) {
           event = new TickEvent.Client.InGame();
        } else {
            event = new TickEvent.Client.OutOfGame();
        }
        ToastClient.EVENT_BUS.post(event);
        if (event.isCancelled()) info.cancel();
    }

    @ModifyVariable(method = "openScreen", at = @At("HEAD"))
    private Screen openScreen(Screen screen) {
        ScreenEvent.Closed closedEvent = new ScreenEvent.Closed(MinecraftClient.getInstance().currentScreen);
        ToastClient.EVENT_BUS.post(closedEvent);
        if (closedEvent.isCancelled()) {
            return MinecraftClient.getInstance().currentScreen;
        }
        ScreenEvent.Displayed displayedEvent = new ScreenEvent.Displayed(screen);
        ToastClient.EVENT_BUS.post(displayedEvent);
        if (displayedEvent.isCancelled()) {
            return MinecraftClient.getInstance().currentScreen;
        }
        return displayedEvent.getScreen();
    }
}
