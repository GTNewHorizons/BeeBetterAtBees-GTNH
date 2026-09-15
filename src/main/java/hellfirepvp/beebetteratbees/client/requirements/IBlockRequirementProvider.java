package hellfirepvp.beebetteratbees.client.requirements;

import java.util.List;

import forestry.api.genetics.IAlleleSpecies;

public interface IBlockRequirementProvider {

    List<BlockRequirement> getRequirements(Object subject, IAlleleSpecies species);
}
