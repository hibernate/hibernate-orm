/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(jakarta.persistence.AccessType.FIELD)
public class Hominidae extends Mammals {
	private int intelligence;

	public int getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(int intelligence) {
		this.intelligence = intelligence;
	}

	public int getNonPersistent() {
		return 0;
	}
}
