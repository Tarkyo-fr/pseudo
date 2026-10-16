package fr.minenorth.pseudo.server;

import fr.minenorth.api.Identity;
import fr.minenorth.api.MineNorth;
import fr.minenorth.pseudo.Labels;
import fr.minenorth.pseudo.PseudoConfig;
import fr.minenorth.pseudo.network.PseudoNetwork;
import fr.minenorth.pseudo.network.PseudoNetwork.NamesPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Envoie à chaque client l'état « en mode staff ? » et, seulement dans ce cas, les noms RP des autres joueurs.
 * Un joueur normal ne reçoit jamais aucun nom RP (rien à fuiter) : son client masque simplement les pseudos.
 * Gère aussi le TAB : nom RP (ou « Inconnu ») pour tous, détail pseudo + couleurs pour le staff en mode staff.
 */
public final class PseudoSync {
    /** Clé NBT posée par minenorth-admin (StaffMode) dans les données persistantes du joueur. */
    private static final String STAFF_KEY = "mnadmin_staffmode";
    private static final int PERIOD_TICKS = 20;

    /** Dernier contenu envoyé à chaque joueur (pour n'envoyer que les changements). */
    private final Map<UUID, Integer> lastSent = new HashMap<>();
    /** Joueurs dont le TAB contient actuellement des noms « staff » (à restaurer en sortant du mode). */
    private final Set<UUID> tabStaff = new HashSet<>();

    public static boolean inStaffMode(ServerPlayer p) {
        var root = p.getPersistentData();
        return root.contains(Player.PERSISTED_NBT_TAG, 10) && root.getCompound(Player.PERSISTED_NBT_TAG).getBoolean(STAFF_KEY);
    }

    /** Joueur sans identité : « Inconnu » dans le TAB (global). Passe après minenorth-identite, qui met le nom RP. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTabName(PlayerEvent.TabListNameFormat e) {
        if (!PseudoConfig.HIDE_TAB.get() || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (MineNorth.identity().get(p.server, p.getUUID()).isEmpty()) {
            e.setDisplayName(Component.literal(PseudoConfig.UNKNOWN_NAME.get()));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % PERIOD_TICKS != 0) return;

        boolean hideNametags = PseudoConfig.HIDE_NAMETAGS.get();
        boolean hideTab = PseudoConfig.HIDE_TAB.get();
        boolean showPseudo = PseudoConfig.STAFF_SHOW_PSEUDO.get();
        int cId = PseudoConfig.color(PseudoConfig.COLOR_IDENTITY.get(), 0xFFFFFF);
        int cNo = PseudoConfig.color(PseudoConfig.COLOR_NO_IDENTITY.get(), 0xFF5555);

        var players = server.getPlayerList().getPlayers();
        Set<UUID> online = new HashSet<>();
        Map<UUID, String> names = null; // calculé une fois, seulement si un staff est connecté

        for (ServerPlayer viewer : players) {
            online.add(viewer.getUUID());
            boolean staff = inStaffMode(viewer);
            Map<UUID, String> out = Map.of();
            if (staff) {
                if (names == null) {
                    Map<UUID, String> built = new HashMap<>();
                    for (ServerPlayer p : players) {
                        MineNorth.identity().get(server, p.getUUID())
                                .map(Identity::fullName).filter(n -> !n.isBlank())
                                .ifPresent(n -> built.put(p.getUUID(), n));
                    }
                    names = built;
                }
                out = new HashMap<>(names);
                out.remove(viewer.getUUID());
            }
            int hash = out.hashCode() * 31 + (staff ? 1 : 0) + (hideNametags ? 2 : 0) + (showPseudo ? 4 : 0) + cId * 7 + cNo * 13;
            Integer prev = lastSent.put(viewer.getUUID(), hash);
            if (prev == null || prev != hash) {
                PseudoNetwork.send(viewer, new NamesPacket(staff, hideNametags, showPseudo, cId, cNo, out));
            }

            // TAB : le staff en mode staff reçoit des noms détaillés (renvoyés à chaque période : un nouvel arrivant écraserait les siens)
            if (staff && hideTab) {
                final Map<UUID, String> rp = names;
                sendTab(viewer, players, p -> Labels.staffLabel(rp.get(p.getUUID()), p.getGameProfile().getName(), showPseudo, cId, cNo));
                tabStaff.add(viewer.getUUID());
            } else if (tabStaff.remove(viewer.getUUID())) {
                sendTab(viewer, players, ServerPlayer::getTabListDisplayName); // retour aux noms globaux
            }
        }
        lastSent.keySet().retainAll(online);
        tabStaff.retainAll(online);
    }

    /** UPDATE_DISPLAY_NAME propre à un seul destinataire (le constructeur public n'accepte pas de nom personnalisé). */
    private static void sendTab(ServerPlayer to, List<ServerPlayer> players, Function<ServerPlayer, Component> name) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                ClientboundPlayerInfoUpdatePacket.Action.class);
        buf.writeVarInt(players.size());
        for (ServerPlayer p : players) {
            buf.writeUUID(p.getUUID());
            buf.writeNullable(name.apply(p), FriendlyByteBuf::writeComponent);
        }
        to.connection.send(new ClientboundPlayerInfoUpdatePacket(buf));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        lastSent.remove(e.getEntity().getUUID());
        tabStaff.remove(e.getEntity().getUUID());
    }
}
