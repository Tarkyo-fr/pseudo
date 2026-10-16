package fr.minenorth.pseudo.client;

import fr.minenorth.pseudo.Labels;
import fr.minenorth.pseudo.network.PseudoNetwork.NamesPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/** Côté client : masque le pseudo des autres joueurs, ou affiche le nom RP si le joueur local est en mode staff. */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ClientNames {
    private ClientNames() {}

    // Défaut sûr tant que le serveur n'a rien envoyé : on masque.
    private static volatile NamesPacket state = new NamesPacket(false, true, true, 0xFFFFFF, 0xFF5555, Map.of());

    public static void set(NamesPacket p) { state = p; }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        NamesPacket s = state;
        if (s.staff()) {
            e.setContent(Labels.staffLabel(s.names().get(p.getUUID()), p.getGameProfile().getName(),
                    s.showPseudo(), s.colorIdentity(), s.colorNoIdentity()));
            e.setResult(Event.Result.ALLOW);
        } else if (s.hideNametags()) {
            e.setResult(Event.Result.DENY);
        }
    }
}
