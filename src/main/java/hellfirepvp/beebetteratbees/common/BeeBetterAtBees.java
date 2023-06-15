package hellfirepvp.beebetteratbees.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 19:50
 * on BeeBetterAtBees
 * BeeBetterAtBees
 */
@Mod(
    modid = BeeBetterAtBees.MODID,
    name = BeeBetterAtBees.NAME,
    version = BeeBetterAtBees.VERSION,
    dependencies = "required-after:NotEnoughItems;required-after:Forestry")
public class BeeBetterAtBees {

    public static final String MODID = "beebetteratbees";
    public static final String NAME = "BeeBetterAtBees";

    public static final String VERSION = Tags.VERSION;

    private static final String PROXY_CLIENT = "hellfirepvp.beebetteratbees.client.ClientProxy";
    private static final String PROXY_SERVER = "hellfirepvp.beebetteratbees.common.CommonProxy";

    public static Logger log = LogManager.getLogger(NAME);

    @Mod.Instance(value = MODID)
    public static BeeBetterAtBees instance;

    @SidedProxy(clientSide = PROXY_CLIENT, serverSide = PROXY_SERVER)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.registerNEIGUIs();
    }

}
