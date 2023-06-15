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
}
