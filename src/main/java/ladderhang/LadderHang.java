package ladderhang;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Лестницы висят на лестнице сверху, как лианы (один якорь на стене — цепочка вниз),
 * и наращиваются вниз правым кликом с лестницей в руке. Серверный мод: клиенту ничего не нужно.
 */
public class LadderHang implements ModInitializer {
	public static final String MOD_ID = "ladderhang";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UseBlockCallback.EVENT.register(LadderAction::onUseBlock);
		LOGGER.info("LadderHang: лестница висит на лестнице сверху, спуск «канатом» включён");

		if (System.getProperty("ladderhang.selftest") != null) {
			ServerTickEvents.START_SERVER_TICK.register(SelfTest::onTick);
			LOGGER.info("LadderHang: включена самопроверка (-Dladderhang.selftest)");
		}
	}
}
