/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

/**
 * Defines a disjunction (OR series).
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see Conjunction
 */
public class Disjunction extends Junction {
	/**
	 * Constructs a Disjunction
	 */
	protected Disjunction() {
		super( Nature.OR );
	}

	protected Disjunction(Criterion[] conditions) {
		super( Nature.OR, conditions );
	}
}
