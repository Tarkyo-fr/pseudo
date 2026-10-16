package fr.minenorth.pseudo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** Texte vu par le staff en mode staff : nom RP + pseudo, ou pseudo seul en couleur « sans identité ». */
public final class Labels {
    private Labels() {}

    public static MutableComponent staffLabel(String rp, String pseudo, boolean showPseudo, int colorIdentity, int colorNoIdentity) {
        if (rp == null) return Component.literal(pseudo).withStyle(s -> s.withColor(TextColor.fromRgb(colorNoIdentity)));
        MutableComponent c = Component.literal(rp).withStyle(s -> s.withColor(TextColor.fromRgb(colorIdentity)));
        if (showPseudo) c.append(Component.literal(" (" + pseudo + ")").withStyle(ChatFormatting.GRAY));
        return c;
    }
}
