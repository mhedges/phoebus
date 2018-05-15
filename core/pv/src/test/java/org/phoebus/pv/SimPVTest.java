/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class SimPVTest extends ThreadHelper
{
/*    @Test
    public void demoSine() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(3);

        final PVListener listener = new PVListener()
        {
            @Override
            public void valueChanged(final PV pv, final VType value)
            {
                System.out.println(pv.getName() + " sent update " + value);
                done.countDown();
            }
        };

        System.out.println("Awaiting " + done.getCount() + " updates...");
        final PV pv = PVPool.getPV("sim://sine");
        pv.addListener(listener);
        done.await();
        pv.removeListener(listener);
        PVPool.releasePV(pv);
    }*/
    
    @Test
    public void demoRxSineBuffer() throws Exception
    {
    	final PV pv = PVPool.getPV("ca://testpv");
		prepareLock(3);
		prepareElapsedTime();

		Disposable disposable = pv.onValueEvent(BackpressureStrategy.BUFFER)
			.buffer(5L, TimeUnit.SECONDS)
			.subscribe(
				list -> {
					System.out.println(attachWithElapsedTime(list.toString()));
					releaseLock();
				},
				throwable -> releaseLock(),
				this::releaseLock);
		
		waitForLock();
		disposable.dispose();
    	PVPool.releasePV(pv);
    }
}
