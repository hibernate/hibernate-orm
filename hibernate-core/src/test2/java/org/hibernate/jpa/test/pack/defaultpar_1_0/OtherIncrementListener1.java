/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.pack.defaultpar_1_0;


/**
 * @author Emmanuel Bernard
 */
public class OtherIncrementListener1 {
	private static int increment;

	public static int getIncrement() {
		return OtherIncrementListener1.increment;
	}

	public static void reset() {
		increment = 0;
	}

	public void increment(Object entity) {
		OtherIncrementListener1.increment++;
	}
}