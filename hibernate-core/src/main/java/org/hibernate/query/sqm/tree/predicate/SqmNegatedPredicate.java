/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class SqmNegatedPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPredicate wrappedPredicate;

	public SqmNegatedPredicate(SqmPredicate wrappedPredicate, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.wrappedPredicate = wrappedPredicate;
	}

	public SqmPredicate getWrappedPredicate() {
		return wrappedPredicate;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return Collections.singletonList( wrappedPredicate );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitNegatedPredicate( this );
	}
}
