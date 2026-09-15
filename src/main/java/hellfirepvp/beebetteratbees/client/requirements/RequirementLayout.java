package hellfirepvp.beebetteratbees.client.requirements;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementLayout {

    private static final int SLOT_SIZE = 18;
    private static final int PITCH = 20;
    private static final int GAP = 4;
    private static final int WIDTH = 166;
    private static final int HEIGHT = 220;

    public static List<RequirementSlot> place(List<BlockRequirement> requirements, int beeX, int beeY,
        List<Rectangle> occupied) {
        List<BlockRequirement> groups = new ArrayList<>();
        for (BlockRequirement requirement : requirements)
            if (requirement != null && !requirement.isEmpty()) groups.add(requirement);
        if (groups.isEmpty()) return Collections.emptyList();
        int width = SLOT_SIZE + (groups.size() - 1) * PITCH;
        int belowY = beeY + SLOT_SIZE + GAP;
        int centeredX = beeX - (width - SLOT_SIZE) / 2;
        List<RequirementSlot> result = tryPlace(groups, centeredX, belowY, occupied);
        if (result == null) result = tryPlace(groups, beeX, belowY, occupied);
        if (result == null) result = scanBelow(groups, beeX, belowY, occupied);
        return result == null ? Collections.<RequirementSlot>emptyList() : result;
    }

    private static List<RequirementSlot> scanBelow(List<BlockRequirement> groups, int beeX, int startY,
        List<Rectangle> occupied) {
        int width = SLOT_SIZE + (groups.size() - 1) * PITCH;
        int centeredX = beeX - (width - SLOT_SIZE) / 2;
        for (int distance = 0; distance <= HEIGHT; distance += PITCH) {
            int y = startY + distance;
            int[] xs = new int[] { centeredX, beeX, beeX - width + SLOT_SIZE };
            for (int x : xs) {
                List<RequirementSlot> result = tryPlace(groups, x, y, occupied);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static List<RequirementSlot> tryPlace(List<BlockRequirement> groups, int x, int y,
        List<Rectangle> occupied) {
        int width = SLOT_SIZE + (groups.size() - 1) * PITCH;
        if (x < 0 || y < 0 || x + width > WIDTH || y + SLOT_SIZE > HEIGHT) return null;
        List<Rectangle> areas = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            Rectangle area = new Rectangle(x + index * PITCH, y, SLOT_SIZE, SLOT_SIZE);
            for (Rectangle taken : occupied) if (taken.intersects(area)) return null;
            areas.add(area);
        }
        List<RequirementSlot> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            Rectangle area = areas.get(index);
            occupied.add(area);
            result.add(new RequirementSlot(groups.get(index), area.x, area.y));
        }
        return result;
    }
}
