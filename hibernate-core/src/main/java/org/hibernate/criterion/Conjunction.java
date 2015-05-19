/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

/**
 * Defines a conjunction (AND series).
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see Disjunction
 */
public class Conjunction extends Junction {
	/**
	 * Constructs a Conjunction
	 */
	public Conjunction() {
		super( Nature.AND );
	}

	protected Conjunction(Criterion... criterion) {
		super( Nature.AND, criterion );
	}
}
