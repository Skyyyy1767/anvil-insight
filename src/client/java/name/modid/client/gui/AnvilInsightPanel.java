package name.modid.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.enchantment.Enchantment;

import name.modid.client.analysis.AnvilAnalysis;
import name.modid.client.analysis.AnvilAnalysis.CostAdjustment;
import name.modid.client.analysis.AnvilAnalysis.EnchantmentChange;
import name.modid.client.analysis.AnvilAnalysis.InvalidReason;
import name.modid.client.analysis.AnvilAnalysis.RejectedEnchantment;
import name.modid.client.analysis.AnvilAnalysis.RenameType;
import name.modid.client.analysis.AnvilAnalysis.RepairType;
import name.modid.client.analysis.AnvilOperationAnalyzer;

import org.jspecify.annotations.Nullable;

public final class AnvilInsightPanel {
	private static final int BACKGROUND_COLOR = 0xF0C6C6C6;
	private static final int BORDER_DARK = 0xFF373737;
	private static final int BORDER_LIGHT = 0xFFFFFFFF;
	private static final int SEPARATOR_COLOR = 0x805A5A5A;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int PRIMARY_COLOR = 0xFF202020;
	private static final int MUTED_COLOR = 0xFF5D5D5D;
	private static final int SUCCESS_COLOR = 0xFF2F7F2F;
	private static final int WARNING_COLOR = 0xFF986300;
	private static final int ERROR_COLOR = 0xFFAA0000;
	private static final int TITLE_HEIGHT = 24;
	private static final int CONTENT_PADDING = 6;
	private static final int SCROLLBAR_GUTTER = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int ENTRY_INDENT = 5;
	private static final int VANILLA_TOOLTIP_WIDTH = 170;
	private static final int VANILLA_SCROLLBAR_WIDTH = 6;
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");
	private static final Identifier SCROLLER_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("widget/scroller_background");

	private AnvilInsightPanel() {
	}

	public static void render(
		final GuiGraphicsExtractor graphics,
		final Font font,
		final PanelBounds bounds,
		final Content content,
		final int scrollOffset,
		final int mouseX,
		final int mouseY
	) {
		drawFrame(graphics, bounds);
		Component title = Component.translatable("anvilinsight.panel.title");
		int titleX = bounds.x() + (bounds.width() - font.width(title)) / 2;
		graphics.text(font, title, titleX, bounds.y() + 7, PRIMARY_COLOR, false);
		graphics.fill(bounds.x() + 4, bounds.y() + 19, bounds.right() - 4, bounds.y() + 20, BORDER_DARK);

		int contentLeft = bounds.x() + CONTENT_PADDING;
		int contentRight = bounds.right() - CONTENT_PADDING - SCROLLBAR_GUTTER;
		int contentTop = bounds.y() + TITLE_HEIGHT;
		int contentBottom = bounds.bottom() - 5;
		int maxScroll = maxScroll(content, bounds);
		int appliedScroll = Math.min(Math.max(0, scrollOffset), maxScroll);
		int y = contentTop - appliedScroll;
		Component hoveredTooltip = null;

		graphics.enableScissor(contentLeft, contentTop, bounds.right() - CONTENT_PADDING, contentBottom);
		for (Row row : content.rows) {
			int rowTop = y;
			int rowBottom = y + row.height();
			if (row.separator()) {
				int separatorY = y + row.height() / 2;
				if (separatorY >= contentTop && separatorY < contentBottom) {
					graphics.fill(contentLeft, separatorY, contentRight, separatorY + 1, SEPARATOR_COLOR);
				}
			} else if (row.text() != null || row.value() != null) {
				int textY = y + row.textOffset();
				if (textY + font.lineHeight >= contentTop && textY < contentBottom) {
					if (row.text() != null) {
						graphics.text(font, row.text(), contentLeft + row.indent(), textY, row.color(), false);
					}
					if (row.value() != null) {
						int valueWidth = font.width(row.value());
						graphics.text(font, row.value(), contentRight - valueWidth, textY, row.valueColor(), false);
					}
				}
			}

			if (row.tooltip() != null
				&& mouseX >= contentLeft && mouseX < contentRight
				&& mouseY >= Math.max(rowTop, contentTop) && mouseY < Math.min(rowBottom, contentBottom)) {
				hoveredTooltip = row.tooltip();
			}
			y = rowBottom;
		}
		graphics.disableScissor();

		if (maxScroll > 0) {
			drawScrollbar(graphics, bounds, content, appliedScroll, maxScroll, contentTop, contentBottom);
		}
		if (hoveredTooltip != null) {
			int tooltipWidth = Math.min(VANILLA_TOOLTIP_WIDTH, Math.max(1, graphics.guiWidth() - 16));
			graphics.setTooltipForNextFrame(font, font.split(hoveredTooltip, tooltipWidth), mouseX, mouseY);
		}
	}

	public static Content createContent(final Font font, final AnvilAnalysis analysis, final int panelWidth) {
		int contentWidth = Math.max(1, panelWidth - CONTENT_PADDING * 2 - SCROLLBAR_GUTTER);
		LayoutBuilder builder = new LayoutBuilder(font, contentWidth);
		createRows(builder, analysis);
		return builder.build();
	}

	public static int maxScroll(final Content content, final PanelBounds bounds) {
		int viewportHeight = bounds.height() - TITLE_HEIGHT - 5;
		return Math.max(0, content.height - viewportHeight);
	}

	private static void drawFrame(final GuiGraphicsExtractor graphics, final PanelBounds bounds) {
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), BACKGROUND_COLOR);
		graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), BORDER_DARK);
		graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.y() + 2, BORDER_LIGHT);
		graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + 2, bounds.bottom() - 1, BORDER_LIGHT);
	}

	private static void drawScrollbar(
		final GuiGraphicsExtractor graphics,
		final PanelBounds bounds,
		final Content content,
		final int scrollOffset,
		final int maxScroll,
		final int contentTop,
		final int contentBottom
	) {
		int trackX = bounds.right() - VANILLA_SCROLLBAR_WIDTH - 2;
		int trackHeight = contentBottom - contentTop;
		int thumbHeight = Math.max(12, trackHeight * trackHeight / content.height);
		int thumbTravel = trackHeight - thumbHeight;
		int thumbY = contentTop + (maxScroll == 0 ? 0 : thumbTravel * scrollOffset / maxScroll);
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, trackX, contentTop, VANILLA_SCROLLBAR_WIDTH, trackHeight
		);
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, trackX, thumbY, VANILLA_SCROLLBAR_WIDTH, thumbHeight
		);
	}

	private static void createRows(final LayoutBuilder builder, final AnvilAnalysis analysis) {
		switch (analysis.state()) {
			case EMPTY -> builder.message("anvilinsight.state.empty");
			case MISSING_BASE_ITEM -> builder.message("anvilinsight.state.missing_base");
			case INCOMPLETE -> builder.message("anvilinsight.state.incomplete");
			case INVALID -> addInvalidRows(builder, analysis);
			case VALID, TOO_EXPENSIVE -> addOperationRows(builder, analysis);
		}
	}

	private static void addInvalidRows(final LayoutBuilder builder, final AnvilAnalysis analysis) {
		builder.wrapped(Component.translatable("anvilinsight.state.invalid"), ERROR_COLOR, 0, 1, null);
		builder.gap(3);
		InvalidReason reason = analysis.invalidReason();
		if (reason != null) {
			String key = switch (reason) {
				case BASE_ITEM_REJECTS_ENCHANTMENTS -> "anvilinsight.invalid.base_item";
				case ITEMS_CANNOT_COMBINE -> "anvilinsight.invalid.items";
				case NOTHING_TO_REPAIR -> "anvilinsight.invalid.repair";
				case NO_COMPATIBLE_ENCHANTMENTS -> "anvilinsight.invalid.enchantments";
			};
			builder.wrapped(Component.translatable(key), TEXT_COLOR, 0, 1, null);
		}
		addRejectedRows(builder, analysis, false);
	}

	private static void addOperationRows(final LayoutBuilder builder, final AnvilAnalysis analysis) {
		if (analysis.state() == AnvilAnalysis.State.TOO_EXPENSIVE) {
			builder.wrapped(Component.translatable("anvilinsight.too_expensive"), ERROR_COLOR, 0, 1, null);
			builder.gap(3);
		}

		builder.section("anvilinsight.section.operation_cost", false);
		int totalCost = analysis.totalCost();
		int limit = AnvilOperationAnalyzer.MAX_SURVIVAL_COST;
		int costColor = totalCost >= AnvilOperationAnalyzer.TOO_EXPENSIVE_THRESHOLD
			? ERROR_COLOR
			: totalCost >= limit - 4 ? WARNING_COLOR : PRIMARY_COLOR;
		builder.wrapped(
			Component.translatable("anvilinsight.cost.relative", totalCost, limit), costColor, 0, 1, null
		);
		if (totalCost < limit) {
			int difference = limit - totalCost;
			String key = difference == 1 ? "anvilinsight.cost.below_limit.one" : "anvilinsight.cost.below_limit.many";
			builder.wrapped(Component.translatable(key, difference), MUTED_COLOR, 0, 1, null);
		} else if (totalCost == limit) {
			builder.wrapped(Component.translatable("anvilinsight.cost.at_limit"), WARNING_COLOR, 0, 1, null);
		} else {
			int difference = totalCost - limit;
			String key = difference == 1 ? "anvilinsight.cost.over_limit.one" : "anvilinsight.cost.over_limit.many";
			builder.wrapped(Component.translatable(key, difference), ERROR_COLOR, 0, 1, null);
		}
		if (analysis.creative() && totalCost >= AnvilOperationAnalyzer.TOO_EXPENSIVE_THRESHOLD) {
			builder.wrapped(Component.translatable("anvilinsight.creative.allowed"), WARNING_COLOR, 0, 1, null);
		}

		if (!analysis.breakdownVerified()) {
			builder.separator();
			builder.wrapped(Component.translatable("anvilinsight.breakdown.syncing"), WARNING_COLOR, 0, 1, null);
			return;
		}

		if (!analysis.enchantmentChanges().isEmpty()) {
			builder.section("anvilinsight.section.enchantments", true);
			for (int i = 0; i < analysis.enchantmentChanges().size(); i++) {
				if (i > 0) {
					builder.gap(3);
				}
				EnchantmentChange change = analysis.enchantmentChanges().get(i);
				builder.wrapped(enchantmentName(change.enchantment().value(), change.resultLevel()), TEXT_COLOR, 0, 1, null);
				Component status;
				int statusColor;
				if (change.oldLevel() == 0) {
					status = Component.translatable("anvilinsight.enchantment.status.added");
					statusColor = SUCCESS_COLOR;
				} else if (change.oldLevel() != change.resultLevel()) {
					status = Component.translatable(
						"anvilinsight.enchantment.status.upgraded", levelName(change.oldLevel()), levelName(change.resultLevel())
					);
					statusColor = SUCCESS_COLOR;
				} else {
					status = Component.translatable("anvilinsight.enchantment.status.retained");
					statusColor = MUTED_COLOR;
				}
				builder.labelValue(status, plus(change.levelCost()), statusColor, TEXT_COLOR, ENTRY_INDENT, null);
			}
		}

		addRejectedRows(builder, analysis, true);

		if (analysis.repair().type() != RepairType.NONE) {
			builder.section("anvilinsight.section.repair", true);
			String repairKey = analysis.repair().type() == RepairType.MATERIAL
				? "anvilinsight.repair.material"
				: "anvilinsight.repair.combined";
			builder.labelValue(
				Component.translatable(repairKey, analysis.repair().durabilityRestored()),
				plus(analysis.repair().levelCost()),
				TEXT_COLOR,
				TEXT_COLOR,
				0,
				null
			);
			if (analysis.repair().materialsConsumed() > 0) {
				builder.wrapped(
					Component.translatable("anvilinsight.repair.materials", analysis.repair().materialsConsumed()),
					MUTED_COLOR,
					ENTRY_INDENT,
					1,
					null
				);
			}
		}

		boolean hasOtherCosts = analysis.priorWorkCost() > 0
			|| analysis.rename().type() != RenameType.NONE
			|| !analysis.adjustments().isEmpty();
		if (hasOtherCosts) {
			builder.section("anvilinsight.section.other_costs", true);
		}
		if (analysis.priorWorkCost() > 0) {
			Component tooltip = Component.translatable("anvilinsight.cost.prior_work.tooltip");
			builder.labelValue(
				Component.translatable("anvilinsight.cost.prior_work"),
				plus(analysis.priorWorkCost()),
				TEXT_COLOR,
				TEXT_COLOR,
				0,
				tooltip
			);
			if (analysis.leftPriorWork() > 0) {
				builder.detailCost("anvilinsight.cost.left_item", analysis.leftPriorWork());
			}
			if (analysis.rightPriorWork() > 0) {
				builder.detailCost("anvilinsight.cost.right_item", analysis.rightPriorWork());
			}
		}
		if (analysis.rename().type() != RenameType.NONE) {
			String key = analysis.rename().type() == RenameType.SET ? "anvilinsight.rename.set" : "anvilinsight.rename.remove";
			builder.labelValue(Component.translatable(key), plus(analysis.rename().levelCost()), TEXT_COLOR, TEXT_COLOR, 0, null);
		}
		for (CostAdjustment adjustment : analysis.adjustments()) {
			String key = switch (adjustment.type()) {
				case STACKED_INPUT -> "anvilinsight.adjustment.stacked_input";
				case RENAME_ONLY_CAP -> "anvilinsight.adjustment.rename_cap";
				case INTEGER_LIMIT -> "anvilinsight.adjustment.integer_limit";
			};
			builder.labelValue(Component.translatable(key), signed(adjustment.amount()), MUTED_COLOR, MUTED_COLOR, 0, null);
		}
	}

	private static void addRejectedRows(final LayoutBuilder builder, final AnvilAnalysis analysis, final boolean showPenalty) {
		if (analysis.rejectedEnchantments().isEmpty()) {
			return;
		}
		builder.section("anvilinsight.section.incompatible", true);
		for (int i = 0; i < analysis.rejectedEnchantments().size(); i++) {
			if (i > 0) {
				builder.gap(3);
			}
			RejectedEnchantment rejected = analysis.rejectedEnchantments().get(i);
			builder.wrapped(enchantmentName(rejected.enchantment().value(), rejected.sourceLevel()), TEXT_COLOR, 0, 1, null);
			Component penalty = showPenalty && rejected.compatibilityPenalty() > 0 ? plus(rejected.compatibilityPenalty()) : null;
			Component tooltip = rejected.compatibilityPenalty() > 0 && showPenalty
				? Component.translatable("anvilinsight.enchantment.penalty.tooltip", rejected.compatibilityPenalty())
				: null;
			builder.labelValue(
				Component.translatable("anvilinsight.enchantment.status.not_applied"),
				penalty,
				ERROR_COLOR,
				WARNING_COLOR,
				ENTRY_INDENT,
				tooltip
			);
			if (rejected.unsupportedByItem()) {
				builder.wrapped(
					Component.translatable("anvilinsight.enchantment.unsupported"), MUTED_COLOR, ENTRY_INDENT, 1, null
				);
			}
			for (var conflict : rejected.conflicts()) {
				builder.wrapped(
					Component.translatable(
						"anvilinsight.enchantment.conflicts", Component.literal(conflict.value().description().getString())
					),
					MUTED_COLOR,
					ENTRY_INDENT,
					1,
					null
				);
			}
		}
	}

	private static Component enchantmentName(final Enchantment enchantment, final int level) {
		Component name = Component.literal(enchantment.description().getString());
		if (level != 1 || enchantment.getMaxLevel() != 1) {
			return Component.empty().append(name).append(" ").append(levelName(level));
		}
		return name;
	}

	private static Component levelName(final int level) {
		return Component.translatable("enchantment.level." + level);
	}

	private static Component plus(final long value) {
		return Component.literal("+" + value);
	}

	private static Component signed(final long value) {
		return Component.literal(value > 0 ? "+" + value : Long.toString(value));
	}

	private record Row(
		@Nullable FormattedCharSequence text,
		@Nullable FormattedCharSequence value,
		int color,
		int valueColor,
		int indent,
		int height,
		int textOffset,
		boolean separator,
		@Nullable Component tooltip
	) {
	}

	private static final class LayoutBuilder {
		private final Font font;
		private final int contentWidth;
		private final List<Row> rows = new ArrayList<>();

		private LayoutBuilder(final Font font, final int contentWidth) {
			this.font = font;
			this.contentWidth = contentWidth;
		}

		private void message(final String key) {
			this.gap(2);
			this.wrapped(Component.translatable(key), TEXT_COLOR, 0, 1, null);
		}

		private void section(final String key, final boolean separated) {
			if (separated) {
				this.separator();
			}
			this.wrapped(Component.translatable(key), MUTED_COLOR, 0, 1, null);
			this.gap(2);
		}

		private void separator() {
			this.gap(3);
			this.rows.add(new Row(null, null, 0, 0, 0, 5, 0, true, null));
			this.gap(3);
		}

		private void detailCost(final String key, final int cost) {
			this.labelValue(
				Component.translatable(key), Component.literal(Integer.toString(cost)), MUTED_COLOR, MUTED_COLOR, ENTRY_INDENT, null
			);
		}

		private void wrapped(
			final Component text,
			final int color,
			final int indent,
			final int lineSpacing,
			final @Nullable Component tooltip
		) {
			int width = Math.max(1, this.contentWidth - indent);
			List<FormattedCharSequence> lines = this.font.split(text, width);
			if (lines.isEmpty()) {
				lines = List.of(Component.empty().getVisualOrderText());
			}
			for (FormattedCharSequence line : lines) {
				this.rows.add(new Row(line, null, color, color, indent, LINE_HEIGHT + lineSpacing, 0, false, tooltip));
			}
		}

		private void labelValue(
			final Component label,
			final @Nullable Component value,
			final int color,
			final int valueColor,
			final int indent,
			final @Nullable Component tooltip
		) {
			int availableWidth = Math.max(1, this.contentWidth - indent);
			int valueWidth = value == null ? 0 : this.font.width(value);
			int gap = value == null ? 0 : 5;
			int labelWidth = this.font.width(label);
			if (labelWidth + gap + valueWidth <= availableWidth) {
				this.rows.add(new Row(
					label.getVisualOrderText(),
					value == null ? null : value.getVisualOrderText(),
					color,
					valueColor,
					indent,
					LINE_HEIGHT + 1,
					0,
					false,
					tooltip
				));
				return;
			}

			List<FormattedCharSequence> lines = this.font.split(label, availableWidth);
			for (FormattedCharSequence line : lines) {
				this.rows.add(new Row(line, null, color, color, indent, LINE_HEIGHT + 1, 0, false, tooltip));
			}
			if (value != null) {
				this.rows.add(new Row(null, value.getVisualOrderText(), valueColor, valueColor, indent, LINE_HEIGHT + 1, 0, false, tooltip));
			}
		}

		private void gap(final int height) {
			if (height > 0) {
				this.rows.add(new Row(null, null, 0, 0, 0, height, 0, false, null));
			}
		}

		private Content build() {
			List<Row> builtRows = List.copyOf(this.rows);
			int height = builtRows.stream().mapToInt(Row::height).sum();
			return new Content(builtRows, height);
		}
	}

	public static final class Content {
		private final List<Row> rows;
		private final int height;

		private Content(final List<Row> rows, final int height) {
			this.rows = rows;
			this.height = height;
		}
	}

	public record PanelBounds(int x, int y, int width, int height) {
		public int right() {
			return this.x + this.width;
		}

		public int bottom() {
			return this.y + this.height;
		}

		public boolean contains(final double mouseX, final double mouseY) {
			return mouseX >= this.x && mouseX < this.right() && mouseY >= this.y && mouseY < this.bottom();
		}
	}
}
