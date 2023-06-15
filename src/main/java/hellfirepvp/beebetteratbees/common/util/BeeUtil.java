package hellfirepvp.beebetteratbees.common.util;

import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;
import hellfirepvp.beebetteratbees.BeeBetterAtBees;
import hellfirepvp.beebetteratbees.common.integration.ModIntegrationJEI;
import net.minecraft.item.ItemStack;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: BeeUtil
 * Created by HellFirePvP
 * Date: 11.10.2018 / 21:36
 */
public class BeeUtil {

    public static List<IBeeMutation> getMutationsWithResult(IAllele allele) {
        if(ModIntegrationJEI.beeRoot == null) return new LinkedList<>();
        LinkedList<IBeeMutation> out = new LinkedList<>();
        for (IBeeMutation mutation : ModIntegrationJEI.beeRoot.getMutations(false)) {
            if(getMutationRoot(mutation).equals(allele)) out.add(mutation);
        }
        return out;
    }

    public static IAlleleBeeSpecies getMutationRoot(IBeeMutation mutation) {
        return (IAlleleBeeSpecies) mutation.getTemplate()[0];
    }

    public static ItemStack createStack(IAlleleSpecies species, EnumBeeType type) {
        ISpeciesRoot root = species.getRoot();
        IAllele[] template = root.getTemplate(species.getUID());
        if (template == null) {
            BeeBetterAtBees.log.warn("Template for %s doesn't exist! Skipping...", species.getUID());
            return ItemStack.EMPTY;
        }
        IIndividual individual = root.templateAsIndividual(template);
        individual.analyze();
        ItemStack stack = root.getMemberStack(individual, type);
        if (stack.isEmpty()) {
            BeeBetterAtBees.log.warn("Got no MemberStack back when creating bee (%s) ?", species.getUID());
        }
        return stack;
    }

}
