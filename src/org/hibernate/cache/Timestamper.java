//$Id$
package org.hibernate.cache;

/**
 * Generates increasing identifiers (in a single VM only).
 * Not valid across multiple VMs. Identifiers are not necessarily
 * strictly increasing, but usually are.
 */
public final class Timestamper {
	private static short counter = 0;
	private static long time;
	private static final int BIN_DIGITS = 12;
	public static final short ONE_MS = 1<<BIN_DIGITS;
	
	public static long next() {
		synchronized(Timestamper.class) {
			long newTime = System.currentTimeMillis() << BIN_DIGITS;
			if (time<newTime) {
				time = newTime;
				counter = 0;
			}
			else if (counter < ONE_MS - 1 ) {
				counter++;
			}
			
			return time + counter;
		}
	}

	private Timestamper() {}
}






