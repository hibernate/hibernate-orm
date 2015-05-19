/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import javax.persistence.MappedSuperclass;
import javax.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@Access(javax.persistence.AccessType.FIELD)
public class LivingBeing {
	boolean isReallyAlive;

	public boolean isReallyAlive() {
		return isReallyAlive;
	}

	public void setReallyAlive(boolean reallyAlive) {
		isReallyAlive = reallyAlive;
	}

	public int nonPersistent() {
		return 0;
	}
}
