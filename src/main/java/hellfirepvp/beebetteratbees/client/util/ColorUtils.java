package hellfirepvp.beebetteratbees.client.util;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("neiaddons");

    public static final ColorResource
    // spotless:off
        neiLineBlack        = color.rgb("neiLineBlack",         "0x000000"),
        neiLineRed          = color.rgb("neiLineRed",           "0xA9000A"),
        neiLineLabelBlack   = color.rgb("neiLineLabelBlack",    "0x000000"),
        neiLineLabelRed     = color.rgb("neiLineLabelRed",      "0xA9000A");
    // spotless:on
}
