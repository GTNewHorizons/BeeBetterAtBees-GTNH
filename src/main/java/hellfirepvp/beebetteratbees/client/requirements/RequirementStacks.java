package hellfirepvp.beebetteratbees.client.requirements;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class RequirementStacks {

    public static List<ItemStack> fromBlock(Block block, int meta) {
        List<ItemStack> result = new ArrayList<>();
        if (block == null || block == Blocks.air) return result;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return result;
        if (meta == OreDictionary.WILDCARD_VALUE) {
            addSubItems(result, item);
        }
        if (result.isEmpty()) add(result, new ItemStack(item, 1, meta == OreDictionary.WILDCARD_VALUE ? 0 : meta));
        return result;
    }

    public static List<ItemStack> sanitize(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        if (stacks != null) for (ItemStack stack : stacks) add(result, stack);
        return result;
    }

    public static void add(List<ItemStack> target, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            int sizeBeforeExpansion = target.size();
            addSubItems(target, stack.getItem());
            if (target.size() == sizeBeforeExpansion) addExact(target, stack.getItem(), 0);
            return;
        }
        addExact(target, stack.getItem(), stack.getItemDamage());
    }

    public static boolean matches(ItemStack candidate, ItemStack ingredient) {
        return candidate != null && ingredient != null
            && candidate.isItemEqual(ingredient)
            && ItemStack.areItemStackTagsEqual(candidate, ingredient);
    }

    public static void addSubItems(List<ItemStack> target, Item item) {
        if (target == null || item == null) return;
        List<ItemStack> variants = new ArrayList<>();
        item.getSubItems(item, null, variants);
        for (ItemStack variant : variants) {
            if (variant != null && variant.getItem() != null
                && variant.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                addExact(target, variant.getItem(), variant.getItemDamage());
            }
        }
    }

    private static void addExact(List<ItemStack> target, Item item, int meta) {
        for (ItemStack existing : target) {
            if (existing.getItem() == item && existing.getItemDamage() == meta) return;
        }
        target.add(new ItemStack(item, 1, meta));
    }
}
