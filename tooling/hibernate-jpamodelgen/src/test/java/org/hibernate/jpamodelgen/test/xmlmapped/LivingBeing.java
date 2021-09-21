/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.xmlmapped;

/**
 * @author Hardy Ferentschik
 */
public class LivingBeing {
	boolean reallyAlive;

	public boolean isReallyAlive() {
		return reallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		this.reallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}
