package name.modid.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Loader-neutral bridge implemented by the anvil screen mixin.
 *
 * <p>This type deliberately lives outside the configured mixin package. Mixin
 * reserves every class below that package for transformation and rejects
 * ordinary application types when Minecraft references them directly.</p>
 */
public interface AnvilInsightScreen {
	void anvilInsight$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

	boolean anvilInsight$mouseClicked(MouseButtonEvent event);

	boolean anvilInsight$mouseDragged(MouseButtonEvent event);

	boolean anvilInsight$mouseReleased(MouseButtonEvent event);

	boolean anvilInsight$mouseScrolled(double mouseX, double mouseY, double verticalAmount);
}
