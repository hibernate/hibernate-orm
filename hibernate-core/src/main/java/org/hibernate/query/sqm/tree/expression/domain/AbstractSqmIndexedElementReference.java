/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmIndexedElementReference
		extends AbstractSpecificSqmElementReference
		implements SqmRestrictedCollectionElementReference {
	private final SqmExpression indexSelectionExpression;
	private final NavigablePath propertyPath;

	public AbstractSqmIndexedElementReference(
			SqmPluralAttributeReference pluralAttributeBinding,
			SqmExpression indexSelectionExpression) {
		super( pluralAttributeBinding );
		this.indexSelectionExpression = indexSelectionExpression;
		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{indexes}" );
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return getPluralAttributeReference();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public CollectionElement getExpressableType() {
		return (CollectionElement) getReferencedNavigable();
	}

	@Override
	public CollectionElement getInferableType() {
		return getExpressableType();
	}

	@Override
	public String asLoggableText() {
		return propertyPath.getFullPath();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getPluralAttributeReference();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most element classifications, the uid should point to the "collection table"...
		return getPluralAttributeReference().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most element classifications, the "identification variable" (alias)
		// 		associated with elements is the identification variable for the collection reference
		return getPluralAttributeReference().getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		// for most element classifications, there is none
		return null;
	}
}
