package hellfirepvp.beebetteratbees.client.requirements;

import java.util.List;

public interface IBlockRequirementProvider {

    List<BlockRequirement> getRequirements(Object subject);
}
