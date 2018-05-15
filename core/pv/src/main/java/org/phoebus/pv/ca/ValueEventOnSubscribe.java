package org.phoebus.pv.ca;

import org.phoebus.vtype.VType;

import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

class ValueEventOnSubscribe implements FlowableOnSubscribe<VType> {
	
	/** JCA Channel */
	private final Channel channel;


	ValueEventOnSubscribe(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void subscribe(FlowableEmitter<VType> emitter) throws Exception {
		ValueEventFlowable listener = new ValueEventFlowable(emitter);
		Monitor monitor = channel.addMonitor(DBRHelper.valueTypeFor(channel),channel.getElementCount(),Monitor.VALUE, listener);
		channel.getContext().flushIO();
		emitter.setCancellable(listener.cancellable(monitor));
	}
}
