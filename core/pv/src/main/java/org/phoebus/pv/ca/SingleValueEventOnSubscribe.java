package org.phoebus.pv.ca;

import org.phoebus.vtype.VType;

import gov.aps.jca.Channel;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

public class SingleValueEventOnSubscribe implements SingleOnSubscribe<VType> {

	private final Channel channel;

	public SingleValueEventOnSubscribe(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void subscribe(SingleEmitter<VType> emitter) throws Exception {
		ValueEventSingle listener = new ValueEventSingle(emitter);
		// ca GetListener doesn't need to be removed, do I need any clean up?
		//emitter.setCancellable(listener.cancellable());
		channel.get(listener);
		channel.getContext().flushIO();
		
	}

}
