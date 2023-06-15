package hellfirepvp.beebetteratbees.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 23:46
 * on BeeBetterAtBees
 * CachedRecipe
 */
public abstract class CachedRecipe {

    final long offset = System.currentTimeMillis();

    public abstract PositionedStack getResult();

    public List<PositionedStack> getIngredients() {
        ArrayList<PositionedStack> stacks = new ArrayList<PositionedStack>();
        PositionedStack stack = getIngredient();
        if (stack != null) {
            stacks.add(stack);
        }
        return stacks;
    }

    public PositionedStack getIngredient() {
        return null;
    }

    public List<PositionedStack> getOtherStacks() {
        ArrayList<PositionedStack> stacks = new ArrayList<PositionedStack>();
        PositionedStack stack = getOtherStack();
        if (stack != null) {
            stacks.add(stack);
        }
        return stacks;
    }

    public PositionedStack getOtherStack() {
        return null;
    }

    public List<PositionedStack> getCycledIngredients(int cycle, List<PositionedStack> ingredients) {
        for (int itemIndex = 0; itemIndex < ingredients.size(); itemIndex++) {
            randomRenderPermutation(ingredients.get(itemIndex), cycle + itemIndex);
        }
        return ingredients;
    }

    public void randomRenderPermutation(PositionedStack stack, long cycle) {
        Random rand = new Random(cycle + this.offset);
        stack.setPermutationToRender(Math.abs(rand.nextInt()) % stack.items.length);
    }

    public void setIngredientPermutation(Collection<PositionedStack> ingredients, ItemStack ingredient) {
        for (PositionedStack stack : ingredients) {
            for (int i = 0; i < stack.items.length; i++) {
                if (NEIServerUtils.areStacksSameTypeCrafting(ingredient, stack.items[i])) {
                    stack.item = stack.items[i];
                    stack.item.setTagCompound(ingredient.getTagCompound());
                    stack.items = new ItemStack[] { stack.item };
                    stack.setPermutationToRender(0);
                    break;
                }
            }
        }
    }

    public boolean contains(Collection<PositionedStack> ingredients, ItemStack ingredient) {
        for (PositionedStack stack : ingredients) {
            if (stack.contains(ingredient)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Collection<PositionedStack> ingredients, Item ingred) {
        for (PositionedStack stack : ingredients) {
            if (stack.contains(ingred)) {
                return true;
            }
        }
        return false;
    }
}
