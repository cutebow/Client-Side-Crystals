package me.clientsidecrystals.compat;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ReplayCrystalPayload(Action action, BlockPos pos, int ttlTicks) implements CustomPayload {
    public static final Id<ReplayCrystalPayload> ID =
            new Id<>(Identifier.of("clientsidecrystals", "flashback_crystal"));
    public static final PacketCodec<RegistryByteBuf, ReplayCrystalPayload> CODEC =
            CustomPayload.codecOf(ReplayCrystalPayload::write, ReplayCrystalPayload::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static ReplayCrystalPayload read(RegistryByteBuf buffer) {
        return new ReplayCrystalPayload(
                Action.fromId(buffer.readByte()),
                BlockPos.PACKET_CODEC.decode(buffer),
                buffer.readVarInt()
        );
    }

    private static void write(ReplayCrystalPayload payload, RegistryByteBuf buffer) {
        buffer.writeByte(payload.action().id());
        BlockPos.PACKET_CODEC.encode(buffer, payload.pos());
        buffer.writeVarInt(Math.max(0, payload.ttlTicks()));
    }

    public enum Action {
        SPAWN((byte) 0),
        REMOVE((byte) 1);

        private final byte id;

        Action(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        private static Action fromId(byte id) {
            return switch (id) {
                case 0 -> SPAWN;
                case 1 -> REMOVE;
                default -> throw new IllegalArgumentException("Unknown replay crystal action id: " + id);
            };
        }
    }
}
