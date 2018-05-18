/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv.vtype_pv;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.vtype.VType;

import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** Implements {@link RuntimePV} for {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypePV implements RuntimePV, Consumer<VType>
{
    private final PV pv;
    private final List<RuntimePVListener> listeners = new CopyOnWriteArrayList<>();
	private final Disposable value_flow;
	private final Disposable connection_flow;
	private final Disposable access_flow;
	private final AtomicBoolean isReadonly = new AtomicBoolean();

    VTypePV(final PV pv)
    {
        this.pv = pv;
        value_flow = pv.onValueEvent(BackpressureStrategy.LATEST).subscribe(this);
        connection_flow = pv.onConnectionEvent(BackpressureStrategy.LATEST).subscribe(connected -> {
            for (RuntimePVListener listener : listeners) {
                if (connected == false) {
                	listener.disconnected(this);
                }
            }
        });
        access_flow = pv.onAccessRightsEvent(BackpressureStrategy.LATEST).subscribe(readonly -> {
        	isReadonly.set(readonly);
        	for (RuntimePVListener listener : listeners) {
                listener.permissionsChanged(this, readonly);
        	}
        });
    }

    @Override
    public String getName()
    {
        return pv.getName();
    }

    @Override
    public void addListener(final RuntimePVListener listener)
    {
        // If there is a known value, perform initial update
        final VType value = pv.onSingleValueEvent().blockingGet();
        if (value != null)
            listener.valueChanged(this, value);
        listeners.add(listener);
    }

    @Override
    public void removeListener(final RuntimePVListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public VType read()
    {
        return pv.onSingleValueEvent().blockingGet();
    }

    @Override
    public boolean isReadonly()
    {
        return isReadonly.get();
    }

    @Override
    public void write(final Object new_value) throws Exception
    {
        try
        {
            pv.setValue(new_value);
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot write " + new_value + " to PV " + getName(), ex);
        }
    }

    PV getPV()
    {
        return pv;
    }

    void close()
    {
    	value_flow.dispose();
    	connection_flow.dispose();
    	access_flow.dispose();
    }

    @Override
    public String toString()
    {
        return getName();
    }

	@Override
	public void accept(VType value) throws Exception {
		for (RuntimePVListener listener : listeners)
            listener.valueChanged(this, value);
	}
}
