package hellfirepvp.beebetteratbees.client.requirements;

import java.util.Collections;

import net.minecraft.util.StatCollector;

import codechicken.nei.PositionedStack;

public class RequirementSlot extends PositionedStack {

    private final String label;

    public RequirementSlot(BlockRequirement requirement, int x, int y) {
        super(requirement.getCandidates(), x, y);
        this.label = requirement.getLabel();
        setTooltip(Collections.singletonList(StatCollector.translateToLocal(label)));
    }

    public String getLabel() {
        return label;
    }

}
