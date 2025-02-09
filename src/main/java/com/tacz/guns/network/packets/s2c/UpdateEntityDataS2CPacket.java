package com.tacz.guns.network.packets.s2c;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.DataEntry;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import com.tacz.guns.util.EnvironmentUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class UpdateEntityDataS2CPacket implements FabricPacket {
    public static final PacketType<UpdateEntityDataS2CPacket> TYPE = PacketType.create(new Identifier(GunMod.MOD_ID, "update_entity_data"), UpdateEntityDataS2CPacket::new);

    private final int entityId;
    private final List<DataEntry<?, ?>> entries;

    public UpdateEntityDataS2CPacket(PacketByteBuf buf) {
        this(buf.readVarInt(), readEntries(buf));
    }

    public UpdateEntityDataS2CPacket(int entityId, List<DataEntry<?, ?>> entries) {
        this.entityId = entityId;
        this.entries = entries;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(entries.size());
        entries.forEach(entry -> entry.write(buf));
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public void handle(PlayerEntity ignoredPlayer, PacketSender ignoredSender) {
        if (EnvironmentUtil.isClient()) {
            onHandle(this);
        }
    }

    @Environment(EnvType.CLIENT)
    private static void onHandle(UpdateEntityDataS2CPacket message) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        Entity entity = world.getEntityById(message.entityId);
        if (entity == null) {
            return;
        }
        SyncedEntityData instance = SyncedEntityData.instance();
        message.entries.forEach(entry -> instance.set(entity, entry.getKey(), entry.getValue()));
    }

    private static List<DataEntry<?, ?>> readEntries(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<DataEntry<?, ?>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(DataEntry.read(buf));
        }
        return entries;
    }
}
