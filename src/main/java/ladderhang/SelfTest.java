package ladderhang;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Самопроверка без клиента: строит лестницы в 2000 блоках от спавна, дёргает правило
 * выживания и {@link LadderAction} за {@code FakePlayer}, пишет результат в лог.
 * Включается только системным свойством {@code -Dladderhang.selftest=1}.
 */
public final class SelfTest {
	private static final BlockPos ORIGIN = new BlockPos(2000, 200, 2000);
	private static final BlockState LADDER_EAST =
			Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);

	private SelfTest() {
	}

	private static boolean done;

	/**
	 * Не SERVER_STARTED: в этот момент часть модов ещё держит null вместо сервера в своих
	 * обработчиках {@link UseBlockCallback} (падал sit), и цепочка события рвётся до нас.
	 * Первый тик — всё уже поднято.
	 */
	public static void onTick(MinecraftServer server) {
		if (done) {
			return;
		}
		done = true;
		run(server);
	}

	public static void run(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			level.setChunkForced(ORIGIN.getX() >> 4, ORIGIN.getZ() >> 4, true);
			level.getChunk(ORIGIN.getX() >> 4, ORIGIN.getZ() >> 4);

			testWallAnchor(level);
			testFloatingAlone(level);
			testHangFromAbove(level);
			testCascade(level);
			testWallKeepsLadder(level);
			testRope(level);
		} catch (Throwable t) {
			LadderHang.LOGGER.error("SELFTEST упал", t);
		}
	}

	/** Якорь на стене: живёт, стена ушла — лестница ушла (ванильное правило не сломали). */
	private static void testWallAnchor(ServerLevel level) {
		BlockPos c = ORIGIN.offset(0, 0, 0);
		clear(level, c.offset(0, -2, 0), 6);
		set(level, c.west(), Blocks.STONE.defaultBlockState());
		set(level, c, LADDER_EAST);
		boolean survives = level.getBlockState(c).canSurvive(level, c);
		set(level, c.west(), Blocks.AIR.defaultBlockState());
		boolean gone = level.getBlockState(c).isAir();
		LadderHang.LOGGER.info("SELFTEST wall-anchor: canSurvive={} после сноса стены пусто={}", survives, gone);
	}

	/** Лестница одна в воздухе — висеть не на чем. */
	private static void testFloatingAlone(ServerLevel level) {
		BlockPos c = ORIGIN.offset(10, 0, 0);
		clear(level, c.offset(0, -2, 0), 6);
		set(level, c, LADDER_EAST);
		boolean survives = level.getBlockState(c).canSurvive(level, c);
		LadderHang.LOGGER.info("SELFTEST floating-alone: canSurvive={} (ожидание false)", survives);
	}

	/** Две лестницы без стены: нижняя держится на верхней. */
	private static void testHangFromAbove(ServerLevel level) {
		BlockPos c = ORIGIN.offset(20, 0, 0);
		clear(level, c.offset(0, -2, 0), 6);
		set(level, c, LADDER_EAST);
		set(level, c.above(), LADDER_EAST);
		boolean lower = level.getBlockState(c).canSurvive(level, c);
		boolean upper = level.getBlockState(c.above()).canSurvive(level, c.above());
		LadderHang.LOGGER.info("SELFTEST hang-from-above: нижняя={} верхняя={} (ожидание true/false)", lower, upper);
	}

	/** Цепочка из 5: сносим верхнюю — осыпается вся. */
	private static void testCascade(ServerLevel level) {
		BlockPos c = ORIGIN.offset(30, 0, 0);
		clear(level, c.offset(0, -2, 0), 12);
		for (int dy = 0; dy < 5; dy++) {
			set(level, c.above(dy), LADDER_EAST);
		}
		int before = ladders(level, c.offset(0, -1, 0), 7);
		set(level, c.above(4), Blocks.AIR.defaultBlockState());
		int after = ladders(level, c.offset(0, -1, 0), 7);
		LadderHang.LOGGER.info("SELFTEST cascade: было={}/5 осталось={} (ожидание 0)", before, after);
	}

	/** Стена держит: сносим верхнюю лестницу — нижняя со стеной остаётся. */
	private static void testWallKeepsLadder(ServerLevel level) {
		BlockPos c = ORIGIN.offset(40, 0, 0);
		clear(level, c.offset(0, -2, 0), 12);
		set(level, c.west(), Blocks.STONE.defaultBlockState());
		set(level, c.west().above(), Blocks.STONE.defaultBlockState());
		set(level, c, LADDER_EAST);
		set(level, c.above(), LADDER_EAST);
		set(level, c.above(), Blocks.AIR.defaultBlockState());
		boolean lower = level.getBlockState(c).is(Blocks.LADDER);
		LadderHang.LOGGER.info("SELFTEST wall-keeps: нижняя на месте={} (ожидание true)", lower);
	}

	/** Спуск канатом: клик по лестнице лестницей наращивает цепочку вниз и тратит предмет. */
	private static void testRope(ServerLevel level) {
		BlockPos c = ORIGIN.offset(50, 0, 0);
		clear(level, c.offset(0, -6, 0), 12);
		for (int dy = 0; dy < 3; dy++) {
			set(level, c.above(dy), LADDER_EAST);
		}
		FakePlayer player = player(level, c.above(1), 3);

		InteractionResult first = click(level, player, c.above(1));
		boolean addedOne = level.getBlockState(c.below()).is(Blocks.LADDER);
		int heldAfterFirst = player.getMainHandItem().getCount();

		InteractionResult second = click(level, player, c.above(1));
		boolean addedTwo = level.getBlockState(c.below(2)).is(Blocks.LADDER);
		int heldAfterSecond = player.getMainHandItem().getCount();

		click(level, player, c.above(1));
		InteractionResult empty = click(level, player, c.above(1));
		boolean stoppedWhenEmpty = !level.getBlockState(c.below(4)).is(Blocks.LADDER);

		int chain = ladders(level, c.below(5), 12);
		LadderHang.LOGGER.info(
				"SELFTEST rope: 1-й={} +1={} в руке={} | 2-й={} +1={} в руке={} | пусто={} стоп={} всего={}",
				first, addedOne, heldAfterFirst, second, addedTwo, heldAfterSecond, empty, stoppedWhenEmpty, chain);
	}

	private static FakePlayer player(ServerLevel level, BlockPos pos, int ladders) {
		FakePlayer player = FakePlayer.get(level);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		player.getAbilities().instabuild = false;
		player.onUpdateAbilities();
		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LADDER, ladders));
		return player;
	}

	private static InteractionResult click(ServerLevel level, FakePlayer player, BlockPos pos) {
		return UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false));
	}

	private static int ladders(ServerLevel level, BlockPos from, int count) {
		int found = 0;
		for (int i = 0; i < count; i++) {
			if (level.getBlockState(from.above(i)).is(Blocks.LADDER)) {
				found++;
			}
		}
		return found;
	}

	private static void clear(ServerLevel level, BlockPos from, int count) {
		for (int i = 0; i < count; i++) {
			set(level, from.above(i), Blocks.AIR.defaultBlockState());
			set(level, from.west().above(i), Blocks.AIR.defaultBlockState());
		}
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, 3);
	}
}
