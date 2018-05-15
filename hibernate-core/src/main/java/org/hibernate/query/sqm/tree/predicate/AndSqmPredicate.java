/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class AndSqmPredicate implements SqmPredicate {
	private final SqmPredicate leftHandPredicate;
	private final SqmPredicate rightHandPredicate;

	public AndSqmPredicate(SqmPredicate leftHandPredicate, SqmPredicate rightHandPredicate) {
		this.leftHandPredicate = leftHandPredicate;
		this.rightHandPredicate = rightHandPredicate;
	}

	public SqmPredicate getLeftHandPredicate() {
		return leftHandPredicate;
	}

	public SqmPredicate getRightHandPredicate() {
		return rightHandPredicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitAndPredicate( this );
	}
}
