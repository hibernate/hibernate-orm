//$Id$
package org.hibernate.ejb.test.pack.war;
import javax.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class IncrementListener {
	private static int increment;

	public static int getIncrement() {
		return increment;
	}

	public static void reset() {
		increment = 0;
	}

	@PrePersist
	public void increment(Object entity) {
		increment++;
	}
}
