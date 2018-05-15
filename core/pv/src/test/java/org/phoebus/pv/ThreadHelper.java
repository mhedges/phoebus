package org.phoebus.pv;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.schedulers.Schedulers;

public abstract class ThreadHelper {

	@Nullable
	private CountDownLatch lock;

	private long elapsedTime;

	protected void prepareLock() {
		prepareLock(1);
	}

	protected void prepareLock(int count) {
		lock = new CountDownLatch(count);
	}

	protected void releaseLock() {
		if (lock == null) {
			return;
		}

		lock.countDown();
	}

	protected boolean waitForLock() {
		if (lock == null) {
			return false;
		}
		try {
			lock.await();
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected String attachWithTid(String text) {
		return "[Tid : " + Thread.currentThread() + "] " + text;
	}

	protected void prepareElapsedTime() {
		elapsedTime = System.currentTimeMillis();
	}

	protected String attachWithElapsedTime(String text) {
		long currentTime = System.currentTimeMillis();
		String timeString = "[Time : " + (currentTime - elapsedTime) + "ms] " + text;
		elapsedTime = currentTime;
		return timeString;
	}

	protected ExecutorService createFixedExecutor(String name, int coreThread, int maxThread) {
		return Executors.newFixedThreadPool(5, new ThreadFactory() {
			AtomicInteger seq = new AtomicInteger(0);

			@Override
			public Thread newThread(@NonNull Runnable r) {
				String threadName = "Executor(" + name + ")#" + seq.incrementAndGet();

				System.out.println(threadName + " has been created.");

				Thread thread = new Thread(r);
				thread.setName(threadName);
				return thread;
			}
		});
	}

	protected Scheduler createFixedScheduler(String name, int coreThread, int maxThread) {
		return Schedulers.from(createFixedExecutor(name, coreThread, maxThread));
	}
}