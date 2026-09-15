package hellfirepvp.beebetteratbees.client.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IMutationCondition;
import forestry.core.genetics.mutations.Mutation;
import forestry.core.genetics.mutations.MutationConditionRequiresResource;
import forestry.core.genetics.mutations.MutationConditionRequiresResourceOreDict;

public class MutationConditionRequirementProvider implements IBlockRequirementProvider {

    @Override
    public List<BlockRequirement> getRequirements(Object subject, IAlleleSpecies species) {
        if (!(subject instanceof Mutation)) return Collections.emptyList();
        List<BlockRequirement> result = new ArrayList<>();
        for (IMutationCondition condition : ((Mutation) subject).getMutationConditions()) {
            List<ItemStack> candidates = candidates(condition);
            if (!candidates.isEmpty()) result.add(new BlockRequirement(candidates, "bbab.requirement.blocks"));
        }
        return result;
    }

    private List<ItemStack> candidates(IMutationCondition condition) {
        if (condition instanceof MutationConditionRequiresResource) {
            ItemStack required = ((MutationConditionRequiresResource) condition).getBlockRequired();
            return RequirementStacks.fromBlock(Block.getBlockFromItem(required.getItem()), required.getItemDamage());
        }
        if (!(condition instanceof MutationConditionRequiresResourceOreDict)) return Collections.emptyList();
        String oreName = OreDictionary
            .getOreName(((MutationConditionRequiresResourceOreDict) condition).getOreDictId());
        if (oreName == null || oreName.isEmpty()) return Collections.emptyList();
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : OreDictionary.getOres(oreName)) RequirementStacks.add(result, stack);
        return result;
    }
}
