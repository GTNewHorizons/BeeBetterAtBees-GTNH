package hellfirepvp.beebetteratbees;

import hellfirepvp.beebetteratbees.common.CommonProxy;
import hellfirepvp.beebetteratbees.common.data.Config;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: BeeBetterAtBees
 * Created by HellFirePvP
 * Date: 10.10.2018 / 22:59
 */
@Mod(modid = BeeBetterAtBees.MODID, name = BeeBetterAtBees.NAME, version = BeeBetterAtBees.VERSION,
        dependencies = "required-after:forge@[14.23.4.2748,);required-after:forestry",
        certificateFingerprint = "a0f0b759d895c15ceb3e3bcb5f3c2db7c582edf0",
        acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true)
public class BeeBetterAtBees {

    public static final String MODID = "beebetteratbees";
    public static final String NAME = "Bee Better at Bees";
    public static final String VERSION = "2.0.3";
    public static final String CLIENT_PROXY = "hellfirepvp.beebetteratbees.client.ClientProxy";
    public static final String COMMON_PROXY = "hellfirepvp.beebetteratbees.common.CommonProxy";

    @Mod.Instance(MODID)
    public static BeeBetterAtBees instance;

    public static Logger log = LogManager.getLogger(NAME);

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        event.getModMetadata().version = VERSION;
        Config.init(event.getSuggestedConfigurationFile());
    }

}
