package com.tacz.guns.network.packets.s2c.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.LogicalSide;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.util.EnvironmentUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class GunReloadS2CPacket implements FabricPacket {
    public static final PacketType<GunReloadS2CPacket> TYPE = PacketType.create(new Identifier(GunMod.MOD_ID, "gun_reload"), GunReloadS2CPacket::new);

    private final int shooterId;
    private final ItemStack gunItemStack;

    public GunReloadS2CPacket(PacketByteBuf buf) {
        this(buf.readVarInt(), buf.readItemStack());
    }

    public GunReloadS2CPacket(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(shooterId);
        buf.writeItemStack(gunItemStack);
    }

    public void handle(PlayerEntity ignoredPlayer, PacketSender ignoredSender) {
        if (EnvironmentUtil.isClient()) {
            doClientEvent(this);
        }
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    private static void doClientEvent(GunReloadS2CPacket message) {
        ClientWorld level = MinecraftClient.getInstance().world;
        if (level == null) {
            return;
        }
        if (level.getEntityById(message.shooterId) instanceof LivingEntity shooter) {
            GunReloadEvent gunReloadEvent = new GunReloadEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
            gunReloadEvent.post();
        }
    }
}
