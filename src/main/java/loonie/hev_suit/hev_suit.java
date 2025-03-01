package loonie.hev_suit;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
	modid = hev_suit.MODID,
	name = hev_suit.NAME,
	version = hev_suit.VERSION
)
public class hev_suit {
	public static final String MODID = "hev_suit";
	public static final String NAME = "HEV Suit Voice System";
	public static final String VERSION = "0.7.0";
	
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	@Mod.EventHandler
	public void preinit(FMLPreInitializationEvent preinit) {
		LOGGER.info("Hello, world!");
	}
}
