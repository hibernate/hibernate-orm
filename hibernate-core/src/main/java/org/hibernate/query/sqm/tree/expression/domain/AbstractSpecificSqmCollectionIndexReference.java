/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSpecificSqmCollectionIndexReference extends AbstractSqmCollectionIndexReference {
	public AbstractSpecificSqmCollectionIndexReference(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public ExpressableType getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMaxIndexFunction( this );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return getPluralAttributeReference().getReferencedNavigable()
				.getPersistentCollectionDescriptor()
				.getIndexDescriptor()
				.createSqmExpression( getPluralAttributeReference().getExportedFromElement(), getPluralAttributeReference(), context );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException(  );
	}
}
