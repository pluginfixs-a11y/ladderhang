package ladderhang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Спуск «канатом»: правый клик лестницей по лестнице наращивает цепочку снизу на один блок.
 * Лестница берётся из руки (в креативе не тратится), звук и взмах — как при обычной установке.
 */
public final class LadderAction {

	private LadderAction() {
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.LADDER)) {
			return InteractionResult.PASS;
		}
		BlockPos clickedPos = hit.getBlockPos();
		BlockState clicked = level.getBlockState(clickedPos);
		if (!clicked.is(Blocks.LADDER)) {
			return InteractionResult.PASS;
		}

		Direction facing = clicked.getValue(LadderBlock.FACING);
		BlockPos bottom = clickedPos;
		while (true) {
			BlockPos below = bottom.below();
			BlockState state = level.getBlockState(below);
			if (!state.is(Blocks.LADDER) || state.getValue(LadderBlock.FACING) != facing) {
				break;
			}
			bottom = below;
		}

		BlockPos target = bottom.below();
		if (!level.isInWorldBounds(target) || !level.getBlockState(target).canBeReplaced()) {
			return InteractionResult.PASS;
		}

		BlockState placed = Blocks.LADDER.defaultBlockState()
				.setValue(LadderBlock.FACING, facing)
				.setValue(LadderBlock.WATERLOGGED, level.getFluidState(target).getType().isSame(Fluids.WATER));
		if (!level.setBlock(target, placed, 3)) {
			return InteractionResult.PASS;
		}

		if (!player.hasInfiniteMaterials()) {
			held.shrink(1);
		}
		level.playSound(null, target, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.swing(hand);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
