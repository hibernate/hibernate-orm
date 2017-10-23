/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.ordered.joinedInheritence;

import javax.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class Tiger extends Animal {
	private int numberOfStripes;

	public int getNumberOfStripes() {
		return numberOfStripes;
	}

	public void setNumberOfStripes(int numberOfStripes) {
		this.numberOfStripes = numberOfStripes;
	}
}
