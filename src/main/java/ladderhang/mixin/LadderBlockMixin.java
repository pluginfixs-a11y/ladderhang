package ladderhang.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ваниль требует твёрдую грань за каждой лестницей. Здесь правило лианы: лестница живёт,
 * если за ней стена ИЛИ сверху висит лестница с тем же FACING.
 * <p>
 * Одного canSurvive мало: ванильный updateShape перепроверяет блок только когда изменился
 * сосед со стороны стены ({@code direction.getOpposite() == FACING}), поэтому снос верхней
 * лестницы не доходил до нижних. Второй инжект добавляет реакцию на соседа сверху — и
 * цепочка осыпается вниз, как у лиан.
 */
@Mixin(LadderBlock.class)
public abstract class LadderBlockMixin {

	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void ladderhang$canHangFromAbove(BlockState state, LevelReader level, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		Direction facing = state.getValue(LadderBlock.FACING);
		BlockState above = level.getBlockState(pos.above());
		if (above.getBlock() == state.getBlock() && above.getValue(LadderBlock.FACING) == facing) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void ladderhang$recheckWhenLadderAboveGoes(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random,
			CallbackInfoReturnable<BlockState> cir) {
		if (direction == Direction.UP && !state.canSurvive(level, pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
