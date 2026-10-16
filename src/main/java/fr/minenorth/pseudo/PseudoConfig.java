package fr.minenorth.pseudo;

import net.minecraft.ChatFormatting;
import net.minecraftforge.common.ForgeConfigSpec;

/** Fichier config/minenorth_pseudo.toml (valeurs lues côté serveur, puis envoyées aux clients). */
public final class PseudoConfig {
    private PseudoConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue HIDE_NAMETAGS;
    public static final ForgeConfigSpec.BooleanValue HIDE_TAB;
    public static final ForgeConfigSpec.BooleanValue STAFF_SHOW_PSEUDO;
    public static final ForgeConfigSpec.ConfigValue<String> UNKNOWN_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_IDENTITY;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_NO_IDENTITY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        HIDE_NAMETAGS = b.comment("Masque les pseudos au-dessus des joueurs (sauf pour le staff en mode staff).")
                .define("hide_nametags", true);
        HIDE_TAB = b.comment("Masque les pseudos dans le TAB (sauf pour le staff en mode staff).")
                .define("hide_tab", true);
        UNKNOWN_NAME = b.comment("Nom affiché dans le TAB pour un joueur sans identité RP.")
                .define("unknown_name", "Inconnu");
        STAFF_SHOW_PSEUDO = b.comment("Le staff en mode staff voit « Prénom Nom (Pseudo) » au lieu du nom RP seul.")
                .define("staff_show_pseudo", true);
        COLOR_IDENTITY = b.comment("Couleur (nom Minecraft ou #RRGGBB) vue par le staff : joueur avec identité.")
                .define("staff_color_identity", "white");
        COLOR_NO_IDENTITY = b.comment("Couleur (nom Minecraft ou #RRGGBB) vue par le staff : joueur SANS identité.")
                .define("staff_color_no_identity", "red");
        SPEC = b.build();
    }

    /** « red », « gold »… ou « #RRGGBB » vers un entier RGB ; {@code def} si invalide. */
    public static int color(String s, int def) {
        if (s == null) return def;
        try {
            if (s.startsWith("#")) return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
            ChatFormatting f = ChatFormatting.getByName(s);
            if (f != null && f.getColor() != null) return f.getColor();
        } catch (NumberFormatException ignored) { }
        return def;
    }
}
