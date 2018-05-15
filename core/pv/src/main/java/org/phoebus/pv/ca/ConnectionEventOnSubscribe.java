package org.phoebus.pv.ca;

import gov.aps.jca.Channel;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

public class ConnectionEventOnSubscribe implements FlowableOnSubscribe<Boolean> {

	/** JCA Channel */
	private volatile Channel channel;
	
	ConnectionEventOnSubscribe(Channel channel) {
		this.channel = channel;
	}
	
	@Override
	public void subscribe(FlowableEmitter<Boolean> emitter) throws Exception {
		ConnectionEventFlowable listener = new ConnectionEventFlowable(emitter);
		channel.addConnectionListener(listener);
		channel.getContext().flushIO();
		emitter.setCancellable(listener.cancellable(channel));
	}

}
