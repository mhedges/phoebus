package org.phoebus.pv.ca;

import org.phoebus.vtype.VType;

import gov.aps.jca.Channel;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

public class AccessRightsEventOnSubscribe implements FlowableOnSubscribe<Boolean> {
	
	/** JCA Channel */
	private volatile Channel channel;
	
	AccessRightsEventOnSubscribe(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void subscribe(FlowableEmitter<Boolean> emitter) throws Exception {
		AccessRightsEventFlowable listener = new AccessRightsEventFlowable (emitter);
		channel.addAccessRightsListener(listener);
		channel.getContext().flushIO();
		emitter.setCancellable(listener.cancellable(channel));
	}

}
