package com.exiledradio.gamedropsbackport.babies;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client, packet id 5: a baby's age lock, sent when a golden dandelion is used on it (with its
 * 2 seconds of particles) and when a player starts seeing a locked baby (without).
 *
 * <p>The handler names no client type: it hands plain values to {@code babies.client.BabiesClient}.
 */
public final class AgeLockMessage implements IMessage {

    int entityId;
    boolean locked;
    boolean particles;

    public AgeLockMessage() {
    }

    AgeLockMessage(int entityId, boolean locked, boolean particles) {
        this.entityId = entityId;
        this.locked = locked;
        this.particles = particles;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.locked = buf.readBoolean();
        this.particles = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.locked);
        buf.writeBoolean(this.particles);
    }

    public static final class Handler implements IMessageHandler<AgeLockMessage, IMessage> {
        @Override
        public IMessage onMessage(AgeLockMessage m, MessageContext ctx) {
            com.exiledradio.gamedropsbackport.babies.client.BabiesClient.ageLock(m.entityId, m.locked, m.particles);
            return null;
        }
    }
}
