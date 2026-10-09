package hellfirepvp.beebetteratbees.client;

import codechicken.nei.api.API;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hellfirepvp.beebetteratbees.client.gui.BBABGuiRecipeTreeHandler;
import hellfirepvp.beebetteratbees.client.requirements.MutationConditionRequirementProvider;
import hellfirepvp.beebetteratbees.client.requirements.RequirementResolvers;
import hellfirepvp.beebetteratbees.common.CommonProxy;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 19:51
 * on BeeBetterAtBees
 * ClientProxy
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerNEIGUIs() {
        registerNEIStuff();
    }

    @SideOnly(Side.CLIENT)
    private void registerNEIStuff() {
        RequirementResolvers.register(new MutationConditionRequirementProvider());
        BBABGuiRecipeTreeHandler handler = new BBABGuiRecipeTreeHandler();
        API.registerRecipeHandler(handler);
        API.registerUsageHandler(handler);
    }

}
