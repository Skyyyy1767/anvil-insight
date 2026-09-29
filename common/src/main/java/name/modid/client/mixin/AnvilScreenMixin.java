package name.modid.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.MouseButtonEvent;

import name.modid.client.gui.AnvilInsightController;
import name.modid.client.gui.AnvilInsightScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin implements AnvilInsightScreen {
	@Shadow
	private EditBox name;

	@Unique
	private AnvilInsightController anvilInsight$controller;

	@Inject(method = "subInit", at = @At("TAIL"))
	private void anvilInsight$initialize(final CallbackInfo ci) {
		this.anvilInsight$controller = AnvilInsightController.attach(
			Minecraft.getInstance(),
			(AnvilScreen)(Object)this,
			this.name,
			widget -> ((ScreenAccessor)this).anvilInsight$addRenderableWidget(widget)
		);
	}

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void anvilInsight$tick(final CallbackInfo ci) {
		if (this.anvilInsight$controller != null) {
			this.anvilInsight$controller.tick();
		}
	}

	@Override
	public void anvilInsight$render(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		if (this.anvilInsight$controller != null) {
			this.anvilInsight$controller.render(graphics, mouseX, mouseY);
		}
	}

	@Override
	public boolean anvilInsight$mouseClicked(final MouseButtonEvent event) {
		return this.anvilInsight$controller != null && this.anvilInsight$controller.mouseClicked(event);
	}

	@Override
	public boolean anvilInsight$mouseDragged(final MouseButtonEvent event) {
		return this.anvilInsight$controller != null && this.anvilInsight$controller.mouseDragged(event);
	}

	@Override
	public boolean anvilInsight$mouseReleased(final MouseButtonEvent event) {
		return this.anvilInsight$controller != null && this.anvilInsight$controller.mouseReleased(event);
	}

	@Override
	public boolean anvilInsight$mouseScrolled(final double mouseX, final double mouseY, final double verticalAmount) {
		return this.anvilInsight$controller != null
			&& this.anvilInsight$controller.mouseScrolled(mouseX, mouseY, verticalAmount);
	}
}
