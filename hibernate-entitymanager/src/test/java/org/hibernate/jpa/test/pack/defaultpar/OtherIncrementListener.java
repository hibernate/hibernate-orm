//$Id$
package org.hibernate.jpa.test.pack.defaultpar;


/**
 * @author Emmanuel Bernard
 */
public class OtherIncrementListener {
	private static int increment;

	public static int getIncrement() {
		return OtherIncrementListener.increment;
	}

	public static void reset() {
		increment = 0;
	}

	public void increment(Object entity) {
		OtherIncrementListener.increment++;
	}
}
