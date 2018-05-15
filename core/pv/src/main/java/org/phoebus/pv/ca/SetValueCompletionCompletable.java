package org.phoebus.pv.ca;

import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;
import io.reactivex.CompletableEmitter;

class SetValueCompletionCompletable implements PutListener {

	private final CompletableEmitter emitter;

	SetValueCompletionCompletable(CompletableEmitter emitter) {
		this.emitter = emitter;
	}

	@Override
	public void putCompleted(PutEvent event) {
		if (emitter.isDisposed()) {
			return;
		}

		if (event.getStatus().isSuccessful()) {
			emitter.onComplete();
		} else {
			emitter.onError(new Exception("write failed: " +event.toString()));
			return;
		}

	}
}
