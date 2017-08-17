/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceAny extends AbstractSqmSingularAttributeReference {
	public SqmSingularAttributeReferenceAny(
			SqmNavigableContainerReference navigableContainerReference,
			SingularPersistentAttribute referencedNavigable) {
		super( navigableContainerReference, referencedNavigable );
	}

	public SqmSingularAttributeReferenceAny(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitAnyValuedSingularAttribute( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return getReferencedNavigable().createQueryResult( expression, resultVariable, creationContext );
	}
}
