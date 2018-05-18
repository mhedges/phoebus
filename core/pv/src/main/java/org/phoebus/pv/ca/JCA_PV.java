package org.phoebus.pv.ca;

import org.phoebus.pv.PV;
import org.phoebus.vtype.VType;

import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public final class JCA_PV extends PV {
	
	private static final short base_priority = ((JCA_Preferences.getInstance().getMonitorMask()
			& Monitor.VALUE) == Monitor.VALUE) ? Channel.PRIORITY_OPI : Channel.PRIORITY_ARCHIVE;

	/** Array with more than LARGE_ARRAY_THRESHOLD elements? */
	private volatile boolean is_large_array = false;


	/** JCA Channel */
	private volatile Channel channel;
	
	public JCA_PV(final String name, String base_name) throws Exception {
		super(name);
		// logger.fine("JCA PV " + base_name);
		// Read-only until connected and we learn otherwise
		// notifyListenersOfPermissions(true);
		base_name = base_name.trim();
		if (base_name.isEmpty())
			throw new Exception("Empty PV name '" + name + "'");

		final short priority = is_large_array ? base_priority : (short) (base_priority + 1);
		channel = JCAContext.getInstance().getContext().createChannel(base_name, priority);
		channel.getContext().flushIO();
	}

	public Flowable<VType> onValueEvent(BackpressureStrategy backpressureStrategy) {
		return Flowable.create(new ValueEventOnSubscribe(channel), backpressureStrategy);
	}
	
	@Override
	public Single<VType> onSingleValueEvent() {
		return Single.create(new SingleValueEventOnSubscribe(channel));
	}

	@Override
	public Completable setValueAsync(Object object) {
		// TODO Auto-generated method stub
		return null; //Completable.create(new SetValueOnSubscribe(channel, value));
	}

	@Override
	public void setValue(Object object) throws Exception {
		// TODO Auto-generated method stub
		
	}

/*	public Completable setValue(Object value) {
		//return Completable.create(new SetValueOnSubscribe(channel, value));
		return null;
	}
	
	public Single<VType> onSingleValueEvent() {
		//return Single.create(new SingleValueEventOnSubscribe(channel));
		return null;
	}

	@Override
	public Completable setValueAsync(VType vType) {
		//return Completable.create(new SetValueOnSubscribe(channel, value));
		return null;
	}

	@Override
	public void setValue(VType vType) {
	    //Completable.create(new SetValueOnSubscribe(channel, value));
	}*/
	
	public Flowable<Boolean> onAccessRightsEvent(BackpressureStrategy backpressureStrategy) {
		return Flowable.create(new AccessRightsEventOnSubscribe(channel), backpressureStrategy);
	}
	
	public Flowable<Boolean> onConnectionEvent (BackpressureStrategy backpressureStrategy) {
		return Flowable.create(new ConnectionEventOnSubscribe(channel), backpressureStrategy);
	}
	
    /** {@inheritDoc} */
    @Override
    protected void close() {
        channel.dispose();
    }
}
