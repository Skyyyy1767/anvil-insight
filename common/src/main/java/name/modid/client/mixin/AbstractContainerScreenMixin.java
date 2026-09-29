package name.modid.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

import name.modid.client.gui.AnvilInsightScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			shift = At.Shift.AFTER
		)
	)
	private void anvilInsight$render(
		final GuiGraphicsExtractor graphics,
		final int mouseX,
		final int mouseY,
		final float partialTick,
		final CallbackInfo ci
	) {
		if ((Object)this instanceof AnvilInsightScreen anvilScreen) {
			anvilScreen.anvilInsight$render(graphics, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void anvilInsight$mouseClicked(
		final MouseButtonEvent event,
		final boolean doubleClick,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if ((Object)this instanceof AnvilInsightScreen anvilScreen && anvilScreen.anvilInsight$mouseClicked(event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void anvilInsight$mouseDragged(
		final MouseButtonEvent event,
		final double horizontalAmount,
		final double verticalAmount,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if ((Object)this instanceof AnvilInsightScreen anvilScreen && anvilScreen.anvilInsight$mouseDragged(event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void anvilInsight$mouseReleased(
		final MouseButtonEvent event,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if ((Object)this instanceof AnvilInsightScreen anvilScreen && anvilScreen.anvilInsight$mouseReleased(event)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void anvilInsight$mouseScrolled(
		final double mouseX,
		final double mouseY,
		final double horizontalAmount,
		final double verticalAmount,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if ((Object)this instanceof AnvilInsightScreen anvilScreen
			&& anvilScreen.anvilInsight$mouseScrolled(mouseX, mouseY, verticalAmount)) {
			cir.setReturnValue(true);
		}
	}
}
