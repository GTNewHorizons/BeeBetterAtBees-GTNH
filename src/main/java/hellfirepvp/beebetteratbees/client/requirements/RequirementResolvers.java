package hellfirepvp.beebetteratbees.client.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import hellfirepvp.beebetteratbees.common.BeeBetterAtBees;

public class RequirementResolvers {

    private static final List<IBlockRequirementProvider> PROVIDERS = new ArrayList<>();

    public static void register(IBlockRequirementProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) PROVIDERS.add(provider);
    }

    public static List<BlockRequirement> resolve(Object subject) {
        List<BlockRequirement> result = new ArrayList<>();
        if (subject == null) return Collections.emptyList();
        for (IBlockRequirementProvider provider : PROVIDERS) {
            try {
                List<BlockRequirement> contributed = provider.getRequirements(subject);
                if (contributed != null) for (BlockRequirement requirement : contributed) {
                    if (requirement != null && !requirement.isEmpty()) result.add(requirement);
                }
            } catch (Throwable error) {
                BeeBetterAtBees.log.warn("Requirement provider failed for {}", subject, error);
            }
        }
        return result;
    }

    public static boolean matches(Object subject, ItemStack ingredient) {
        if (ingredient == null) return false;
        for (BlockRequirement requirement : resolve(subject)) {
            for (ItemStack candidate : requirement.getCandidates()) {
                if (RequirementStacks.matches(candidate, ingredient)) return true;
            }
        }
        return false;
    }
}
