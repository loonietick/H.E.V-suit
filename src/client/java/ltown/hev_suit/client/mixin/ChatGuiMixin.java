package ltown.hev_suit.client.mixin;

import ltown.hev_suit.client.config.ChatOffsetConfig;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


    @Mixin(ChatHud.class)
    public class ChatGuiMixin {
        
        @ModifyArg(
            method = "render",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I"
            ),
            index = 3 // Y position parameter
        )
        private int adjustTextPosition(int originalY) {
            return originalY - ChatOffsetConfig.getChatOffset();
        }
    }