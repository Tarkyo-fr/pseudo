package fr.minenorth.pseudo;

import fr.minenorth.pseudo.network.PseudoNetwork;
import fr.minenorth.pseudo.server.PseudoSync;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Masque les pseudos Minecraft au-dessus des joueurs et dans le TAB. Le staff en mode staff (minenorth-admin) voit à la
 * place « Prénom Nom (Pseudo) ». Le nom RP dans le chat reste géré par minenorth-identite.
 */
@Mod(MineNorthPseudo.MOD_ID)
public class MineNorthPseudo {
    public static final String MOD_ID = "minenorthpseudo";

    public MineNorthPseudo() {
        PseudoNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PseudoConfig.SPEC, "minenorth_pseudo.toml");
        MinecraftForge.EVENT_BUS.register(new PseudoSync());
    }
}
