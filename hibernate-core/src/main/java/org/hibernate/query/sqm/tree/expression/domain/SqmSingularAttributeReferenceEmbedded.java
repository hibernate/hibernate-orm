/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEmbedded
		extends AbstractSqmSingularAttributeReference
		implements SqmEmbeddableTypedReference {
	public SqmSingularAttributeReferenceEmbedded(
			SqmNavigableContainerReference domainReferenceBinding,
			SingularPersistentAttributeEmbedded boundNavigable) {
		super( domainReferenceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceEmbedded(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public SingularPersistentAttributeEmbedded getReferencedNavigable() {
		return (SingularPersistentAttributeEmbedded) super.getReferencedNavigable();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEmbeddableValuedSingularAttribute( this );
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
