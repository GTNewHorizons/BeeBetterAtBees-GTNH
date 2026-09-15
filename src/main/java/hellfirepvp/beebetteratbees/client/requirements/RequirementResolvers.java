package hellfirepvp.beebetteratbees.client.requirements;

import java.util.ArrayList;
import java.util.List;

import forestry.api.genetics.IAlleleSpecies;
import hellfirepvp.beebetteratbees.common.BeeBetterAtBees;

public class RequirementResolvers {

    private static final List<IBlockRequirementProvider> PROVIDERS = new ArrayList<>();

    public static void register(IBlockRequirementProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) PROVIDERS.add(provider);
    }

    public static List<BlockRequirement> resolve(Object subject, IAlleleSpecies species) {
        List<BlockRequirement> result = new ArrayList<>();
        if (subject == null) return result;
        for (IBlockRequirementProvider provider : PROVIDERS) {
            try {
                List<BlockRequirement> contributed = provider.getRequirements(subject, species);
                if (contributed != null) for (BlockRequirement requirement : contributed) {
                    if (requirement != null && !requirement.isEmpty()) result.add(requirement);
                }
            } catch (Throwable error) {
                BeeBetterAtBees.log.warn("Requirement provider failed for {}", subject, error);
            }
        }
        return result;
    }
}
