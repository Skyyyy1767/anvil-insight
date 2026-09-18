package name.modid.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int anvilInsight$getLeftPos();

	@Accessor("leftPos")
	void anvilInsight$setLeftPos(int leftPos);

	@Accessor("topPos")
	int anvilInsight$getTopPos();

	@Accessor("imageWidth")
	int anvilInsight$getImageWidth();

	@Accessor("imageHeight")
	int anvilInsight$getImageHeight();

	@Accessor("titleLabelX")
	int anvilInsight$getTitleLabelX();

	@Accessor("titleLabelY")
	int anvilInsight$getTitleLabelY();
}
