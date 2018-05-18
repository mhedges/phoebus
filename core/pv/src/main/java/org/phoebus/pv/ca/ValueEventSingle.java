package org.phoebus.pv.ca;

import org.phoebus.vtype.VType;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import io.reactivex.SingleEmitter;

public class ValueEventSingle implements GetListener {

	private final SingleEmitter<VType> emitter;

	public ValueEventSingle(SingleEmitter<VType> emitter) {
		this.emitter = emitter;
	}

	@Override
	public void getCompleted(GetEvent event) {
		try
        {   // May receive event with null status when 'disconnected'
            final CAStatus status = event.getStatus();
            if (status != null  &&  status.isSuccessful())
            {
            	Channel channel = (Channel)event.getSource();
                final int elements = channel.getElementCount();
                boolean is_array = elements != 1;
                final VType value = DBRHelper.decodeValue(is_array, event.getDBR(), event.getDBR());
                //logger.log(Level.FINE, "{0} = {1}", new Object[] { getName(), value });
                emitter.onSuccess(value);
            }
        }
        catch (Exception ex)
        {
            //logger.log(Level.WARNING, getName() + " monitor error", ex);
        	emitter.onError(ex);
            //+ex.printStackTrace();
        }
	}
}
