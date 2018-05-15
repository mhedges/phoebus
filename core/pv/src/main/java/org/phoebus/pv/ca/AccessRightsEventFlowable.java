package org.phoebus.pv.ca;

import gov.aps.jca.Channel;
import gov.aps.jca.event.AccessRightsEvent;
import gov.aps.jca.event.AccessRightsListener;
import io.reactivex.FlowableEmitter;
import io.reactivex.functions.Cancellable;

public class AccessRightsEventFlowable implements AccessRightsListener, TypeCancellable <Channel> {
	
	private final FlowableEmitter<Boolean> emitter;

	AccessRightsEventFlowable(FlowableEmitter<Boolean> emitter){
		this.emitter = emitter;
	}

	@Override
	public void accessRightsChanged(AccessRightsEvent event) {
		final boolean readonly = ! event.getWriteAccess();
		emitter.onNext(readonly);
		
	}

	@Override
	public Cancellable cancellable(Channel channel) {

		return new AccessRightsEventCancellable(channel, this);
	}

}
