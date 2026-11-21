package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.gamedropsbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Packet 20: whether the player is in the sulfur caves, sent when it changes. */
public final class SulfurNet {

    /** On the client: the server's last word. */
    public static volatile boolean inside;

    private SulfurNet() {
    }

    static void tell(EntityPlayerMP player, boolean in) {
        NetworkHandler.channel.sendTo(new InsideMessage(in), player);
    }

    public static final class InsideMessage implements IMessage {
        boolean in;

        public InsideMessage() {
        }

        InsideMessage(boolean in) {
            this.in = in;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.in = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(this.in);
        }
    }

    public static final class Handler implements IMessageHandler<InsideMessage, IMessage> {
        @Override
        public IMessage onMessage(InsideMessage message, MessageContext ctx) {
            inside = message.in;
            return null;
        }
    }
}
