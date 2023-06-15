package hellfirepvp.beebetteratbees.client;

import codechicken.nei.api.IConfigureNEI;
import hellfirepvp.beebetteratbees.client.gui.BBABGuiRecipeTreeHandler;
import hellfirepvp.beebetteratbees.common.BeeBetterAtBees;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 22:59
 * on BeeBetterAtBees
 * NEIBBABConfig
 */
public class NEIBBABConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        BBABGuiRecipeTreeHandler.loadBeeRoot();
    }

    @Override
    public String getName() {
        return BeeBetterAtBees.NAME;
    }

    @Override
    public String getVersion() {
        return BeeBetterAtBees.VERSION;
    }

    /*

    Are you FOR REAL NEI?!?!
    The class has to be called NEI_SOMETHING_Config ?!? WTF?

    ClassDiscoverer classDiscoverer = new ClassDiscoverer(new IStringMatcher() {
      public boolean matches(String test) {
        return (test.startsWith("NEI")) && (test.endsWith("Config.class"));
      }
    }, new Class[] { IConfigureNEI.class });
     */

}
