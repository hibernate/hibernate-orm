/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceBasic extends AbstractSqmSingularAttributeReference {
	public SqmSingularAttributeReferenceBasic(
			SqmNavigableContainerReference sourceBinding,
			BasicSingularPersistentAttribute boundNavigable) {
		super( sourceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceBasic(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public BasicSingularPersistentAttribute getReferencedNavigable() {
		return (BasicSingularPersistentAttribute) super.getReferencedNavigable();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public String getUniqueIdentifier() {
		return null;
	}

	@Override
	public String getIdentificationVariable() {
		return null;
	}

	@Override
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBasicValuedSingularAttribute( this );
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
