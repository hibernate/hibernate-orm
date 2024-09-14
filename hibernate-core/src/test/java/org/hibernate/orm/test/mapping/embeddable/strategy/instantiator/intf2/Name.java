/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf2;

/**
 * @author Steve Ebersole
 */
public class Name {
	private final String first;
	private final String last;

	public static Name make(String first, String last) {
		return new Name( first, last );
	}

	private Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	String getFirstName() {
		return first;
	}

	String getLastName() {
		return last;
	}
}
