// -*- java -*-
package eem.frame.misc;

public class timer {
	private long startTime = -1;
	private long allowedTime = -1;

	public timer(long allowedTime) {
		startTime = System.nanoTime();
		this.allowedTime = allowedTime;
	}

	public void start() {
		startTime = System.nanoTime();
	}

	public long timeLeft() {
		return allowedTime - (System.nanoTime() - startTime);
	}
}

