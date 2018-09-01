package net.tomp2p.connection;

import net.tomp2p.message.Message;
import net.tomp2p.rpc.RPC;

/**
 * Default sending behavior for UDP and TCP messages. Depending whether the
 * recipient is relayed, slow and on the message size, decisions can be made
 * here.
 * 
 * @author Nico Rutishauser
 * 
 */
public class DefaultSendBehavior implements SendBehavior {

	private static final int MTU = 1000;
	
	@Override
	public SendMethod tcpSendBehavior(Message message) {
		if(message.recipient().peerId().equals(message.sender().peerId())) {
			// shortcut, just send to yourself
			return SendMethod.SELF;
		}

		if (message.recipient().isRelayed()) {
			if (message.sender().isRelayed()) {
				// reverse connection is not possible because both peers are
				// relayed. Thus send the message to
				// one of the receiver's relay peers
				return SendMethod.RELAY;
			} else if (message.recipient().isSlow()) {
				// the recipient is a slow peer (i.e. a mobile device). Send it
				// to the relay such that this
				// one can handle latency and buffer multiple requests
				return SendMethod.RELAY;
			} else {
				// Messages with small size can be sent over relay, other messages should be sent directly (more efficient)
				if(message.estimateSize() > MTU) {
					return SendMethod.RCON;
				} else {
					return SendMethod.RELAY;
				}
			}
		} else {
			// send directly
			return SendMethod.DIRECT;
		}
	}

	@Override
	public SendMethod udpSendBehavior(Message message) throws UnsupportedOperationException {
		if(message.recipient().peerId().equals(message.sender().peerId())) {
			// shortcut, just send to yourself
			return SendMethod.SELF;
		}

		if (message.recipient().isRelayed() && message.sender().isRelayed()
				&& !(message.command() == RPC.Commands.NEIGHBOR.getNr() || message.command() == RPC.Commands.PING.getNr())) {
			return SendMethod.HOLEP;
		} else if (message.recipient().isRelayed()) {
			if (message.command() == RPC.Commands.NEIGHBOR.getNr() || message.command() == RPC.Commands.PING.getNr()) {
				return SendMethod.RELAY;
			} else {
				throw new UnsupportedOperationException(
						"Tried to send UDP message to unreachable peers. Only TCP messages can be sent to unreachable peers");
			}
		} else {
			return SendMethod.DIRECT;
		}
	}
}
