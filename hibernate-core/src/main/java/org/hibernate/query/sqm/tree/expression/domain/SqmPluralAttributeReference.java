/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * Specialization of a "Navigable reference" for plural attributes.
 *
 * @author Steve Ebersole
 */
public class SqmPluralAttributeReference
		extends AbstractSqmAttributeReference<PluralPersistentAttribute>
		implements SqmNavigableContainerReference {

	public SqmPluralAttributeReference(SqmNavigableContainerReference lhs, PluralPersistentAttribute attribute) {
		super( lhs, attribute );
	}

	public SqmPluralAttributeReference(SqmAttributeJoin join) {
		super( join );
	}

	@Override
	public SqmAttributeJoin getExportedFromElement() {
		return (SqmAttributeJoin) super.getExportedFromElement();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistentCollectionMetadata().getPersistenceType();
	}

	@Override
	public String getUniqueIdentifier() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public String getIdentificationVariable() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttribute( this );
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
