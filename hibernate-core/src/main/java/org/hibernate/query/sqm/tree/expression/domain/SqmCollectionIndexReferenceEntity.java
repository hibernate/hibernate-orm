/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmCollectionIndexReferenceEntity
		extends AbstractSqmCollectionIndexReference
		implements SqmNavigableContainerReference, SqmEntityTypedReference {
	private SqmFrom exportedFromElement;

	public SqmCollectionIndexReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public EntityValuedNavigable getReferencedNavigable() {
		return (CollectionIndexEntity) getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getIndexDescriptor();
	}

	@Override
	public EntityValuedExpressableType getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		// todo (6.0) : override this to account for implicit or explicit Downcasts
		return super.getIntrinsicSubclassEntityMetadata();
	}

	@Override
	public String getUniqueIdentifier() {
		// todo (6.0) : for the entity index classification we should point to the referenced entity's uid
		return super.getUniqueIdentifier();
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
				.createSqmExpression( exportedFromElement, getPluralAttributeReference(), context );
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
