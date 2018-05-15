package org.phoebus.pv.ca;

import io.reactivex.FlowableEmitter;
import io.reactivex.functions.Cancellable;

import org.phoebus.vtype.VType;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_LABELS_Enum;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.DBR_TIME_String;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

public class ValueEventFlowable implements MonitorListener, ConnectionListener, TypeCancellable <Monitor> {

	private final FlowableEmitter<VType> emitter;
	
    /** Threshold above which arrays use a lower channel priority
     *  (idea from PVManager)
     */
    private static final int LARGE_ARRAY_THRESHOLD = JCA_Preferences.getInstance().largeArrayThreshold();
    
    /** Array with more than LARGE_ARRAY_THRESHOLD elements? */
    private volatile boolean is_large_array = false;
	
    /** Channel Access does not really distinguish between array and scalar.
     *  An array may at times only have one value, like a scalar.
     *  To get more consistent decoding, channels with a max. element count other
     *  than 1 are considered arrays.
     */
    private volatile boolean is_array = false;
    
    /** Meta data.
    *
    *  <p>May be
    *  <ul>
    *  <li>null
    *  <li>DBR_CTRL_Double, DBR_CTRL_INT, ..BYTE, which all implement CTRL and TIME
    *  <li>DBR_CTRL_String, DBR_CTRL_Enum which are each different
    *  </ul>
    */
   private volatile DBR metadata = null;
	
	ValueEventFlowable(FlowableEmitter<VType> emitter) {
	    this.emitter = emitter;
	}
	

	@Override
	public void monitorChanged(MonitorEvent event) {
		try
        {   // May receive event with null status when 'disconnected'
            final CAStatus status = event.getStatus();
            if (status != null  &&  status.isSuccessful())
            {
            	
                final VType value = DBRHelper.decodeValue(is_array, metadata, event.getDBR());
                //logger.log(Level.FINE, "{0} = {1}", new Object[] { getName(), value });
                emitter.onNext(value);
            }
        }
        catch (Exception ex)
        {
            //logger.log(Level.WARNING, getName() + " monitor error", ex);
        	emitter.onError(ex);
            //+ex.printStackTrace();
        }
		
	}
	
	@Override
	public Cancellable cancellable(Monitor monitor) {
		
		return new ValueEventCancellable(monitor, this);
	}


	@Override
	public void connectionChanged(ConnectionEvent event) {
    	if (event.isConnected()) {
            //logger.log(Level.FINE, "{0} connected", getName());
    		Channel channel = (Channel)event.getSource();
            final int elements = channel.getElementCount();
            is_array = elements != 1;
            /*if (elements > LARGE_ARRAY_THRESHOLD  &&  ! is_large_array)
            {
                is_large_array = true;
                final String name = channel.getName();
                channel.dispose();
                //logger.log(Level.FINE, "Reconnecting large array {0} at lower priority", name);
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
            }*/
            try
            {
                // With very old IOCs, could only get one element for Ctrl type.
                // With R3.15.5, fetching just one element for a record.INP$
                // (i.e. fetching the string as a BYTE[])
                // crashes the IOC.
                // --> Using the same request count as for the subscription
                final int request_count = JCAContext.getInstance().getRequestCount(channel);
                boolean plain_dbr = DBRHelper.rtypeStringPattern.matcher(channel.getName()).matches();
                channel.get(DBRHelper.getCtrlType(plain_dbr, channel.getFieldType()), request_count, meta_get_listener);
                channel.getContext().flushIO();
            }
            catch (Exception ex)
            {
            	emitter.onError(ex);
                //logger.log(Level.WARNING, getName() + " cannot get meta data", ex);
            }
        }
		
	}
	
	/** Listener to initial get-callback for meta data */
    final private GetListener meta_get_listener = (GetEvent ev) ->
    {
        final DBR old_metadata = metadata;
        final Class<?> old_type = old_metadata == null ? null : old_metadata.getClass();
        // Channels from CAS, not based on records, may fail
        // to provide meta data
        if (ev.getStatus().isSuccessful())
        {
            metadata = ev.getDBR();
            //logger.log(Level.FINE, "{0} received meta data: {1}", new Object[] { getName(), metadata });
        }
        else
        {
            metadata = null;
            //logger.log(Level.FINE, "{0} has no meta data: {1}", new Object[] { getName(), ev.getStatus() });
        }
        // If channel changed its type, cancel potentially existing subscription
        final Class<?> new_type = metadata == null ? null : metadata.getClass();
        //if (old_type != new_type)
            //unsubscribe();
        // Subscribe, either for the first time or because type changed requires new one.
        // NOP if channel is already subscribed.
        //subscribe();
    };
}
