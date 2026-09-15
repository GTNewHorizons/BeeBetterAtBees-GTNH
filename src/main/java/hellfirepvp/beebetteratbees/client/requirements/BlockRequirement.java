package hellfirepvp.beebetteratbees.client.requirements;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

public class BlockRequirement {

    private final List<ItemStack> candidates;
    private final String label;

    public BlockRequirement(List<ItemStack> candidates, String label) {
        this.candidates = Collections.unmodifiableList(RequirementStacks.sanitize(candidates));
        this.label = label;
    }

    public List<ItemStack> getCandidates() {
        return candidates;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEmpty() {
        return candidates.isEmpty();
    }
}
