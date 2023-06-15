package hellfirepvp.beebetteratbees.common.integration.jei;

import hellfirepvp.beebetteratbees.BeeBetterAtBees;
import hellfirepvp.beebetteratbees.common.integration.ModIntegrationJEI;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: CategoryBeeTree
 * Created by HellFirePvP
 * Date: 11.10.2018 / 20:19
 */
public class CategoryBeeTree implements IRecipeCategory<BeeTreeRecipeWrapper> {

    private final IDrawable background;

    public CategoryBeeTree(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(BeeBetterAtBees.MODID, "textures/jei/blank.png");
        background = guiHelper.createDrawable(location, 0, 0, 210, 180);
    }

    @Override
    public String getUid() {
        return ModIntegrationJEI.idBeeTree;
    }

    @Override
    public String getTitle() {
        return I18n.format("jei.beebetteratbees.tree");
    }

    @Override
    public String getModName() {
        return BeeBetterAtBees.NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, BeeTreeRecipeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup grp = recipeLayout.getItemStacks();

        int idx = 0;
        grp.init(idx++, false, recipeWrapper.getRootStack().posX, recipeWrapper.getRootStack().posY);

        for (BeeTreeRecipeWrapper.PosItemStack in : recipeWrapper.getEvaluatedBeePositions()) {
            grp.init(idx++, true, in.posX, in.posY);
        }

        grp.set(ingredients);
    }
}
