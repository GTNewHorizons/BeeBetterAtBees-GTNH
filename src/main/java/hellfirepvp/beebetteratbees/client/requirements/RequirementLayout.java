package hellfirepvp.beebetteratbees.client.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementLayout {

    public static final int SLOT_SIZE = 18;
    public static final int SLOT_PITCH = 20;
    public static final int SLOT_GAP = 6;
    public static final int GUI_WIDTH = 166;
    public static final int GUI_HEIGHT = 220;
    public static final int MIN_RECIPE_HEIGHT = GUI_HEIGHT;

    public static List<RequirementSlot> place(List<BlockRequirement> requirements, int beeX, int beeY) {
        List<BlockRequirement> groups = new ArrayList<>();
        for (BlockRequirement requirement : requirements)
            if (requirement != null && !requirement.isEmpty()) groups.add(requirement);
        if (groups.isEmpty()) return Collections.emptyList();
        int width = SLOT_SIZE + (groups.size() - 1) * SLOT_PITCH;
        int belowY = beeY + SLOT_SIZE + SLOT_GAP;
        int centeredX = Math.max(0, Math.min(GUI_WIDTH - width, beeX - (width - SLOT_SIZE) / 2));
        return placeRow(groups, centeredX, belowY);
    }

    private static List<RequirementSlot> placeRow(List<BlockRequirement> groups, int x, int y) {
        int width = SLOT_SIZE + (groups.size() - 1) * SLOT_PITCH;
        if (x < 0 || y < 0 || x + width > GUI_WIDTH || y + SLOT_SIZE > GUI_HEIGHT) {
            return Collections.emptyList();
        }
        List<RequirementSlot> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            result.add(new RequirementSlot(groups.get(index), x + index * SLOT_PITCH, y));
        }
        return result;
    }
}
