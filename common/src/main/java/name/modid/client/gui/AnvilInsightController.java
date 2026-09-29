package name.modid.client.gui;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import name.modid.client.AnvilInsightClient;
import name.modid.client.analysis.AnvilAnalysis;
import name.modid.client.analysis.AnvilOperationAnalyzer;
import name.modid.client.gui.AnvilInsightPanel.Content;
import name.modid.client.gui.AnvilInsightPanel.PanelBounds;
import name.modid.client.gui.AnvilInsightPanel.ScrollbarGeometry;
import name.modid.client.mixin.AbstractContainerScreenAccessor;

public final class AnvilInsightController {
	private static final Component ICON = Component.literal("i");
	private static final int GAP = 4;
	private static final int SCREEN_MARGIN = 4;
	private static final int PREFERRED_PANEL_WIDTH = 176;
	private static final int BUTTON_SIZE = 14;

	private final Minecraft client;
	private final AnvilScreen screen;
	private final AbstractContainerScreenAccessor screenAccessor;
	private final EditBox nameBox;
	private Button insightButton;
	private PanelBounds panelBounds = new PanelBounds(0, 0, 0, 0);
	private AnvilAnalysis analysis;
	private Content panelContent;
	private ItemStack lastInput = ItemStack.EMPTY;
	private ItemStack lastAddition = ItemStack.EMPTY;
	private int lastCost = Integer.MIN_VALUE;
	private String lastName = "";
	private boolean lastCreative;
	private int scrollOffset;
	private boolean draggingScrollbar;
	private double scrollbarDragOffset;

	private AnvilInsightController(final Minecraft client, final AnvilScreen screen, final EditBox nameBox) {
		this.client = client;
		this.screen = screen;
		this.screenAccessor = (AbstractContainerScreenAccessor)screen;
		this.nameBox = nameBox;
	}

	public static AnvilInsightController attach(
		final Minecraft client,
		final AnvilScreen screen,
		final EditBox nameBox,
		final Consumer<AbstractWidget> addWidget
	) {
		AnvilInsightController controller = new AnvilInsightController(client, screen, nameBox);
		controller.initialize(addWidget);
		return controller;
	}

	private void initialize(final Consumer<AbstractWidget> addWidget) {
		this.insightButton = new InsightButton(button -> {
			AnvilInsightClient.togglePanel();
			this.scrollOffset = 0;
			this.draggingScrollbar = false;
			this.updateLayout();
			this.updateButtonTooltip();
		});
		addWidget.accept(this.insightButton);
		this.updateLayout();
		this.updateButtonTooltip();
		this.refreshAnalysisIfNeeded();
	}

	public void tick() {
		this.refreshAnalysisIfNeeded();
	}

	public void render(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		this.refreshAnalysisIfNeeded();
		if (AnvilInsightClient.isPanelOpen() && this.panelContent != null) {
			AnvilInsightPanel.render(
				graphics, this.screen.getFont(), this.panelBounds, this.panelContent, this.scrollOffset, mouseX, mouseY
			);
		}
	}

	public boolean mouseClicked(final MouseButtonEvent event) {
		if (!AnvilInsightClient.isPanelOpen() || !this.panelBounds.contains(event.x(), event.y())) {
			return false;
		}

		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.panelContent != null) {
			ScrollbarGeometry scrollbar = AnvilInsightPanel.scrollbarGeometry(
				this.panelContent, this.panelBounds, this.scrollOffset
			);
			if (scrollbar.maxScroll() > 0 && scrollbar.containsThumb(event.x(), event.y())) {
				this.draggingScrollbar = true;
				this.scrollbarDragOffset = event.y() - scrollbar.thumbY();
			}
		}
		return true;
	}

	public boolean mouseDragged(final MouseButtonEvent event) {
		if (!this.draggingScrollbar || event.button() != InputConstants.MOUSE_BUTTON_LEFT || this.panelContent == null) {
			return false;
		}

		ScrollbarGeometry scrollbar = AnvilInsightPanel.scrollbarGeometry(
			this.panelContent, this.panelBounds, this.scrollOffset
		);
		double thumbPosition = event.y() - this.scrollbarDragOffset - scrollbar.trackTop();
		double clampedPosition = Math.max(0.0, Math.min(scrollbar.thumbTravel(), thumbPosition));
		this.scrollOffset = scrollbar.thumbTravel() == 0
			? 0
			: (int)Math.round(clampedPosition * scrollbar.maxScroll() / scrollbar.thumbTravel());
		return true;
	}

	public boolean mouseReleased(final MouseButtonEvent event) {
		if (!this.draggingScrollbar || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}

		this.draggingScrollbar = false;
		return true;
	}

	public boolean mouseScrolled(final double mouseX, final double mouseY, final double verticalAmount) {
		if (!AnvilInsightClient.isPanelOpen() || !this.panelBounds.contains(mouseX, mouseY) || this.panelContent == null) {
			return false;
		}

		int maxScroll = AnvilInsightPanel.maxScroll(this.panelContent, this.panelBounds);
		int direction = verticalAmount > 0.0 ? -1 : verticalAmount < 0.0 ? 1 : 0;
		this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset + direction * 20));
		return direction != 0;
	}

	private void updateLayout() {
		int imageWidth = this.screenAccessor.anvilInsight$getImageWidth();
		int imageHeight = this.screenAccessor.anvilInsight$getImageHeight();
		int naturalLeft = (this.screen.width - imageWidth) / 2;
		int left = naturalLeft;
		int panelWidth = PREFERRED_PANEL_WIDTH;

		if (AnvilInsightClient.isPanelOpen()) {
			int naturalRightSpace = this.screen.width - (naturalLeft + imageWidth) - SCREEN_MARGIN - GAP;
			if (naturalRightSpace < PREFERRED_PANEL_WIDTH) {
				int availablePanelWidth = Math.max(1, this.screen.width - imageWidth - GAP - SCREEN_MARGIN * 2);
				panelWidth = Math.min(PREFERRED_PANEL_WIDTH, availablePanelWidth);
				int combinedWidth = imageWidth + GAP + panelWidth;
				left = Math.max(0, (this.screen.width - combinedWidth) / 2);
			}
		}

		this.screenAccessor.anvilInsight$setLeftPos(left);
		this.nameBox.setX(left + 62);
		int top = this.screenAccessor.anvilInsight$getTopPos();
		this.insightButton.setPosition(left + imageWidth - BUTTON_SIZE - 4, top + 5);
		int previousPanelWidth = this.panelBounds.width();
		this.panelBounds = new PanelBounds(left + imageWidth + GAP, top, panelWidth, imageHeight);
		if (this.analysis != null && previousPanelWidth != panelWidth) {
			this.panelContent = AnvilInsightPanel.createContent(this.screen.getFont(), this.analysis, panelWidth);
			this.scrollOffset = Math.min(this.scrollOffset, AnvilInsightPanel.maxScroll(this.panelContent, this.panelBounds));
		}
	}

	private void updateButtonTooltip() {
		String key = AnvilInsightClient.isPanelOpen() ? "anvilinsight.button.hide" : "anvilinsight.button.show";
		this.insightButton.setTooltip(Tooltip.create(Component.translatable(key)));
	}

	private void refreshAnalysisIfNeeded() {
		AnvilMenu menu = this.screen.getMenu();
		ItemStack input = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack addition = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		int cost = menu.getCost();
		String requestedName = this.nameBox.getValue();
		boolean creative = this.client.player != null && this.client.player.hasInfiniteMaterials();
		if (ItemStack.matches(input, this.lastInput)
			&& ItemStack.matches(addition, this.lastAddition)
			&& cost == this.lastCost
			&& requestedName.equals(this.lastName)
			&& creative == this.lastCreative) {
			return;
		}

		this.lastInput = input.copy();
		this.lastAddition = addition.copy();
		this.lastCost = cost;
		this.lastName = requestedName;
		this.lastCreative = creative;
		this.scrollOffset = 0;
		if (this.client.player != null) {
			this.analysis = AnvilOperationAnalyzer.analyze(menu, this.client.player, requestedName);
			this.panelContent = AnvilInsightPanel.createContent(this.screen.getFont(), this.analysis, this.panelBounds.width());
		}
	}

	private static final class InsightButton extends Button {
		private InsightButton(final OnPress onPress) {
			super(
				0,
				0,
				BUTTON_SIZE,
				BUTTON_SIZE,
				ICON,
				onPress,
				ignored -> AbstractWidget.wrapDefaultNarrationMessage(Component.translatable("anvilinsight.button.insight"))
			);
		}

		@Override
		protected void extractContents(
			final net.minecraft.client.gui.GuiGraphicsExtractor graphics,
			final int mouseX,
			final int mouseY,
			final float partialTick
		) {
			this.extractDefaultSprite(graphics);
			this.extractDefaultLabel(graphics.textRendererForWidget(
				this,
				net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.NONE
			));
		}
	}
}
