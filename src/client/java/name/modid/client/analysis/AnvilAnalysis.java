package name.modid.client.analysis;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jspecify.annotations.Nullable;

public record AnvilAnalysis(
	State state,
	int totalCost,
	int simulatedCost,
	boolean breakdownVerified,
	boolean creative,
	int leftPriorWork,
	int rightPriorWork,
	Repair repair,
	Rename rename,
	List<EnchantmentChange> enchantmentChanges,
	List<RejectedEnchantment> rejectedEnchantments,
	List<CostAdjustment> adjustments,
	@Nullable InvalidReason invalidReason
) {
	public long priorWorkCost() {
		return (long)this.leftPriorWork + this.rightPriorWork;
	}

	public boolean hasOperation() {
		return this.state == State.VALID || this.state == State.TOO_EXPENSIVE;
	}

	public enum State {
		EMPTY,
		MISSING_BASE_ITEM,
		INCOMPLETE,
		INVALID,
		VALID,
		TOO_EXPENSIVE
	}

	public enum InvalidReason {
		BASE_ITEM_REJECTS_ENCHANTMENTS,
		ITEMS_CANNOT_COMBINE,
		NOTHING_TO_REPAIR,
		NO_COMPATIBLE_ENCHANTMENTS
	}

	public enum RepairType {
		NONE,
		MATERIAL,
		COMBINED_ITEMS
	}

	public enum RenameType {
		NONE,
		SET,
		REMOVE
	}

	public enum AdjustmentType {
		STACKED_INPUT,
		RENAME_ONLY_CAP,
		INTEGER_LIMIT
	}

	public record Repair(RepairType type, int durabilityRestored, int materialsConsumed, int levelCost) {
		public static final Repair NONE = new Repair(RepairType.NONE, 0, 0, 0);
	}

	public record Rename(RenameType type, int levelCost) {
		public static final Rename NONE = new Rename(RenameType.NONE, 0);
	}

	public record EnchantmentChange(Holder<Enchantment> enchantment, int oldLevel, int sourceLevel, int resultLevel, int levelCost) {
	}

	public record RejectedEnchantment(
		Holder<Enchantment> enchantment,
		int sourceLevel,
		boolean unsupportedByItem,
		List<Holder<Enchantment>> conflicts,
		int compatibilityPenalty
	) {
	}

	public record CostAdjustment(AdjustmentType type, long amount) {
	}
}
