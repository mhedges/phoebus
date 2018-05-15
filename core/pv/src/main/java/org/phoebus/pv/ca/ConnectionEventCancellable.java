package org.phoebus.pv.ca;

import gov.aps.jca.Channel;
import gov.aps.jca.event.ConnectionListener;
import io.reactivex.functions.Cancellable;

public class ConnectionEventCancellable implements Cancellable {

	private final Channel channel;
	private final ConnectionListener listener;

	ConnectionEventCancellable(Channel channel, ConnectionListener listener) {
	    this.channel = channel;
	    this.listener = listener;
	  }

	@Override
	public void cancel() throws Exception {
		channel.removeConnectionListener(listener);
	}
}
