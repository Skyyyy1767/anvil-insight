package name.modid.client.analysis;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import name.modid.client.analysis.AnvilAnalysis.AdjustmentType;
import name.modid.client.analysis.AnvilAnalysis.CostAdjustment;
import name.modid.client.analysis.AnvilAnalysis.EnchantmentChange;
import name.modid.client.analysis.AnvilAnalysis.InvalidReason;
import name.modid.client.analysis.AnvilAnalysis.RejectedEnchantment;
import name.modid.client.analysis.AnvilAnalysis.Rename;
import name.modid.client.analysis.AnvilAnalysis.RenameType;
import name.modid.client.analysis.AnvilAnalysis.Repair;
import name.modid.client.analysis.AnvilAnalysis.RepairType;
import name.modid.client.analysis.AnvilAnalysis.State;

public final class AnvilOperationAnalyzer {
	public static final int TOO_EXPENSIVE_THRESHOLD = 40;
	public static final int MAX_SURVIVAL_COST = TOO_EXPENSIVE_THRESHOLD - 1;

	private AnvilOperationAnalyzer() {
	}

	public static AnvilAnalysis analyze(final AnvilMenu menu, final Player player, final String requestedName) {
		ItemStack input = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack addition = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		int vanillaCost = menu.getCost();
		boolean creative = player.hasInfiniteMaterials();

		if (input.isEmpty()) {
			State state = addition.isEmpty() ? State.EMPTY : State.MISSING_BASE_ITEM;
			return emptyAnalysis(state, vanillaCost, creative, null);
		}

		if (!EnchantmentHelper.canStoreEnchantments(input)) {
			return emptyAnalysis(State.INVALID, vanillaCost, creative, InvalidReason.BASE_ITEM_REJECTS_ENCHANTMENTS);
		}

		Simulation simulation = simulate(input, addition, player, requestedName);
		State state;
		if (simulation.invalidReason != null) {
			state = State.INVALID;
		} else if (vanillaCost <= 0) {
			state = State.INCOMPLETE;
		} else if (vanillaCost >= TOO_EXPENSIVE_THRESHOLD && !creative) {
			state = State.TOO_EXPENSIVE;
		} else {
			state = State.VALID;
		}

		boolean verified = simulation.finalCost == vanillaCost && simulation.explainedCost == simulation.finalCost;
		return new AnvilAnalysis(
			state,
			vanillaCost,
			simulation.finalCost,
			verified,
			creative,
			simulation.leftPriorWork,
			simulation.rightPriorWork,
			simulation.repair,
			simulation.rename,
			List.copyOf(simulation.changes),
			List.copyOf(simulation.rejected),
			List.copyOf(simulation.adjustments),
			simulation.invalidReason
		);
	}

	private static AnvilAnalysis emptyAnalysis(
		final State state, final int vanillaCost, final boolean creative, final InvalidReason invalidReason
	) {
		return new AnvilAnalysis(
			state,
			vanillaCost,
			0,
			vanillaCost == 0,
			creative,
			0,
			0,
			Repair.NONE,
			Rename.NONE,
			List.of(),
			List.of(),
			List.of(),
			invalidReason
		);
	}

	private static Simulation simulate(final ItemStack input, final ItemStack addition, final Player player, final String requestedName) {
		Simulation result = new Simulation();
		ItemStack output = input.copy();
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(output));
		result.leftPriorWork = input.getOrDefault(DataComponents.REPAIR_COST, 0);
		result.rightPriorWork = addition.getOrDefault(DataComponents.REPAIR_COST, 0);
		long priorWork = (long)result.leftPriorWork + result.rightPriorWork;
		int price = 0;
		int namingCost = 0;

		if (!addition.isEmpty()) {
			boolean usingStoredEnchantments = addition.has(DataComponents.STORED_ENCHANTMENTS);
			if (output.isDamageableItem() && input.isValidRepairItem(addition)) {
				int damageBefore = output.getDamageValue();
				int repairAmount = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
				if (repairAmount <= 0) {
					result.invalidReason = InvalidReason.NOTHING_TO_REPAIR;
					return result;
				}

				int count;
				for (count = 0; repairAmount > 0 && count < addition.getCount(); count++) {
					output.setDamageValue(output.getDamageValue() - repairAmount);
					price++;
					repairAmount = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
				}

				result.repair = new Repair(RepairType.MATERIAL, damageBefore - output.getDamageValue(), count, count);
			} else {
				if (!usingStoredEnchantments && (!output.is(addition.getItem()) || !output.isDamageableItem())) {
					result.invalidReason = InvalidReason.ITEMS_CANNOT_COMBINE;
					return result;
				}

				if (output.isDamageableItem() && !usingStoredEnchantments) {
					int damageBefore = output.getDamageValue();
					int remainingInput = input.getMaxDamage() - input.getDamageValue();
					int remainingAddition = addition.getMaxDamage() - addition.getDamageValue();
					int bonus = remainingAddition + output.getMaxDamage() * 12 / 100;
					int combinedDamage = Math.max(0, output.getMaxDamage() - (remainingInput + bonus));
					if (combinedDamage < output.getDamageValue()) {
						output.setDamageValue(combinedDamage);
						price += 2;
						result.repair = new Repair(RepairType.COMBINED_ITEMS, damageBefore - combinedDamage, 0, 2);
					}
				}

				ItemEnchantments additionalEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(addition);
				boolean anyCompatible = false;
				boolean anyRejected = false;

				for (Entry<Holder<Enchantment>> entry : additionalEnchantments.entrySet()) {
					Holder<Enchantment> enchantmentHolder = entry.getKey();
					int oldLevel = enchantments.getLevel(enchantmentHolder);
					int sourceLevel = entry.getIntValue();
					int combinedLevel = oldLevel == sourceLevel ? sourceLevel + 1 : Math.max(sourceLevel, oldLevel);
					Enchantment enchantment = enchantmentHolder.value();
					boolean unsupported = !enchantment.canEnchant(input);
					boolean compatible = !unsupported;
					if (player.hasInfiniteMaterials() || input.is(Items.ENCHANTED_BOOK)) {
						compatible = true;
						unsupported = false;
					}

					List<Holder<Enchantment>> conflicts = new ArrayList<>();
					for (Holder<Enchantment> other : enchantments.keySet()) {
						if (!other.equals(enchantmentHolder) && !Enchantment.areCompatible(enchantmentHolder, other)) {
							compatible = false;
							conflicts.add(other);
							price++;
						}
					}

					if (!compatible) {
						anyRejected = true;
						result.rejected.add(
							new RejectedEnchantment(enchantmentHolder, sourceLevel, unsupported, List.copyOf(conflicts), conflicts.size())
						);
					} else {
						anyCompatible = true;
						int appliedLevel = Math.min(combinedLevel, enchantment.getMaxLevel());
						enchantments.set(enchantmentHolder, appliedLevel);
						int fee = enchantment.getAnvilCost();
						if (usingStoredEnchantments) {
							fee = Math.max(1, fee / 2);
						}

						int enchantmentCost = fee * appliedLevel;
						price += enchantmentCost;
						result.changes.add(new EnchantmentChange(enchantmentHolder, oldLevel, sourceLevel, appliedLevel, enchantmentCost));
						if (input.getCount() > 1) {
							int adjustment = TOO_EXPENSIVE_THRESHOLD - price;
							price = TOO_EXPENSIVE_THRESHOLD;
							if (adjustment != 0) {
								result.adjustments.add(new CostAdjustment(AdjustmentType.STACKED_INPUT, adjustment));
							}
						}
					}
				}

				if (anyRejected && !anyCompatible) {
					result.invalidReason = InvalidReason.NO_COMPATIBLE_ENCHANTMENTS;
					result.finalCost = 0;
					return result;
				}
			}
		}

		if (!StringUtil.isBlank(requestedName)) {
			if (!requestedName.equals(input.getHoverName().getString())) {
				namingCost = 1;
				price++;
				result.rename = new Rename(RenameType.SET, 1);
			}
		} else if (input.has(DataComponents.CUSTOM_NAME)) {
			namingCost = 1;
			price++;
			result.rename = new Rename(RenameType.REMOVE, 1);
		}

		int finalCost = price <= 0 ? 0 : (int)Mth.clamp(priorWork + price, 0L, Integer.MAX_VALUE);
		if (namingCost == price && namingCost > 0 && finalCost >= TOO_EXPENSIVE_THRESHOLD) {
			result.adjustments.add(new CostAdjustment(AdjustmentType.RENAME_ONLY_CAP, MAX_SURVIVAL_COST - finalCost));
			finalCost = MAX_SURVIVAL_COST;
		}

		long explained = priorWork + result.repair.levelCost() + namingCost;
		for (EnchantmentChange change : result.changes) {
			explained += change.levelCost();
		}
		for (RejectedEnchantment rejected : result.rejected) {
			explained += rejected.compatibilityPenalty();
		}
		for (CostAdjustment adjustment : result.adjustments) {
			explained += adjustment.amount();
		}

		if (price > 0 && explained != finalCost) {
			long adjustment = (long)finalCost - explained;
			result.adjustments.add(new CostAdjustment(AdjustmentType.INTEGER_LIMIT, adjustment));
			explained += adjustment;
		}

		result.finalCost = finalCost;
		result.explainedCost = price > 0 ? explained : 0L;
		return result;
	}

	private static final class Simulation {
		private int finalCost;
		private long explainedCost;
		private int leftPriorWork;
		private int rightPriorWork;
		private Repair repair = Repair.NONE;
		private Rename rename = Rename.NONE;
		private final List<EnchantmentChange> changes = new ArrayList<>();
		private final List<RejectedEnchantment> rejected = new ArrayList<>();
		private final List<CostAdjustment> adjustments = new ArrayList<>();
		private InvalidReason invalidReason;
	}
}
