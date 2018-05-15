package org.phoebus.pv.ca;


import gov.aps.jca.Monitor;
import gov.aps.jca.event.MonitorListener;
import io.reactivex.functions.Cancellable;

class ValueEventCancellable implements Cancellable {

  private final Monitor monitor;
  private final MonitorListener listener;

  ValueEventCancellable(Monitor monitor, MonitorListener listener) {
    this.monitor = monitor;
    this.listener = listener;
  }

  @Override
  public void cancel() throws Exception {
	  monitor.removeMonitorListener(listener);
	  monitor.clear();
  }
}
