package org.phoebus.pv.ca;

import gov.aps.jca.Channel;
import gov.aps.jca.event.AccessRightsListener;
import io.reactivex.functions.Cancellable;

public class AccessRightsEventCancellable implements Cancellable {

	private final Channel channel;
	private final AccessRightsListener listener;

	AccessRightsEventCancellable(Channel channel, AccessRightsListener listener) {
	    this.channel = channel;
	    this.listener = listener;
	  }
	@Override
	public void cancel() throws Exception {
		channel.removeAccessRightsListener(listener);
	}

}
