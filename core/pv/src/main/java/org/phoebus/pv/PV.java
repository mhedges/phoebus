package org.phoebus.pv;

import java.util.logging.Logger;

import org.phoebus.vtype.VType;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public abstract class PV {
	
	public static final Logger logger = Logger.getLogger(PV.class.getName());
	
	private String name;
	
	/** Initialize
     *  @param name PV name
     */
    protected PV(final String name)
    {
        this.name = name;
    }

    /** @return PV name */
    public String getName()
    {
        return name;
    }

	abstract public Flowable<VType> onValueEvent(BackpressureStrategy backpressureStrategy);

/*	abstract public Single<VType> onSingleValueEvent();
	
	abstract public Completable setValueAsync(VType vType);
	
	abstract public void setValue(VType vType);*/
	
	abstract public Flowable<Boolean> onAccessRightsEvent(BackpressureStrategy backpressureStrategy);
	
	abstract public Flowable<Boolean> onConnectionEvent (BackpressureStrategy backpressureStrategy);
	
    /** Close the PV, releasing underlying resources.
     *  <p>
     *  Called by {@link PVPool}.
     *  Users of this class should instead release PV from pool.
     *
     *  @see PVPool#releasePV(PV)
     */
    protected void close()
    {
        // Default implementation has nothing to close
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " '" + getName();
    }
}
