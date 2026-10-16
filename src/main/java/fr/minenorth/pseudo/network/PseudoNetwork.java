package fr.minenorth.pseudo.network;

import fr.minenorth.pseudo.MineNorthPseudo;
import fr.minenorth.pseudo.client.ClientNames;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class PseudoNetwork {
    private PseudoNetwork() {}

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthPseudo.MOD_ID, "network"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, NamesPacket.class, NamesPacket::encode, NamesPacket::decode, NamesPacket::handle);
    }

    public static void send(ServerPlayer to, NamesPacket p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), p);
    }

    /**
     * staff = le destinataire est en mode staff ; names = noms RP des autres joueurs (vide si staff = false).
     * Les autres champs viennent de la config serveur.
     */
    public record NamesPacket(boolean staff, boolean hideNametags, boolean showPseudo, int colorIdentity, int colorNoIdentity,
                              Map<UUID, String> names) {
        static void encode(NamesPacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.staff);
            b.writeBoolean(p.hideNametags);
            b.writeBoolean(p.showPseudo);
            b.writeInt(p.colorIdentity);
            b.writeInt(p.colorNoIdentity);
            b.writeVarInt(p.names.size());
            p.names.forEach((id, n) -> { b.writeUUID(id); b.writeUtf(n, 128); });
        }

        static NamesPacket decode(FriendlyByteBuf b) {
            boolean staff = b.readBoolean(), hide = b.readBoolean(), show = b.readBoolean();
            int ci = b.readInt(), cn = b.readInt();
            int n = Math.min(b.readVarInt(), 2048);
            Map<UUID, String> m = new HashMap<>();
            for (int i = 0; i < n; i++) m.put(b.readUUID(), b.readUtf(128));
            return new NamesPacket(staff, hide, show, ci, cn, m);
        }

        static void handle(NamesPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNames.set(p)));
            c.get().setPacketHandled(true);
        }
    }
}
