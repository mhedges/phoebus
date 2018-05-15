package org.phoebus.pv.ca;

import gov.aps.jca.Channel;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import io.reactivex.FlowableEmitter;
import io.reactivex.functions.Cancellable;

public class ConnectionEventFlowable implements ConnectionListener, TypeCancellable <Channel>  {

	private final FlowableEmitter<Boolean> emitter;

	ConnectionEventFlowable(FlowableEmitter<Boolean> emitter) {
	    this.emitter = emitter;
	}
	
	@Override
	public void connectionChanged(ConnectionEvent event) {
		if (event.isConnected())
        {
			emitter.onNext(true);
/*            logger.log(Level.FINE, "{0} connected", getName());

            final int elements = channel.getElementCount();
            is_array = elements != 1;
            if (elements > LARGE_ARRAY_THRESHOLD  &&  ! is_large_array)
            {
                is_large_array = true;
                final String name = channel.getName();
                channel.dispose();
                logger.log(Level.FINE, "Reconnecting large array {0} at lower priority", name);
                channel = null;
                try
                {
                    createChannel(name);
                }
                catch (Exception ex)
                {
                    logger.log(Level.SEVERE, "Cannot re-create channel for large array", ex);
                }
                return;
            }

            final boolean is_readonly = ! channel.getWriteAccess();
            notifyListenersOfPermissions(is_readonly);
            getMetaData(); // .. and start subscription
*/        }
        else
        {
        	emitter.onNext(false);
/*            logger.fine(getName() + " disconnected");
            notifyListenersOfDisconnect();
            // On re-connect, fetch meta data
            // and maybe re-subscribe (possibly for changed type after IOC reboot)
*/        }
		
	}

	@Override
	public Cancellable cancellable(Channel channel) {

		return new ConnectionEventCancellable(channel, this);
	}

}
