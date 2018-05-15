package org.phoebus.pv.ca;

import io.reactivex.functions.Cancellable;

interface TypeCancellable<T> {

  Cancellable cancellable(T t);

}
