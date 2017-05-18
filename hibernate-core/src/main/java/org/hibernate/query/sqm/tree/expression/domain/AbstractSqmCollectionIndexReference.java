/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata.CollectionClassification;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCollectionIndexReference
		extends AbstractSqmNavigableReference
		implements SqmCollectionIndexReference {

	private final SqmPluralAttributeReference attributeBinding;
	private final PluralPersistentAttribute pluralAttributeReference;
	private final NavigablePath propertyPath;

	public AbstractSqmCollectionIndexReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeBinding = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		assert pluralAttributeReference.getPersistentCollectionMetadata().getCollectionClassification() == CollectionClassification.MAP
				|| pluralAttributeReference.getPersistentCollectionMetadata().getCollectionClassification() == CollectionClassification.LIST;

		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{keys}" );
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public ExpressableType getExpressionType() {
		return getPluralAttributeBinding().getReferencedNavigable().getPersistentCollectionMetadata().getIndexDescriptor();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return attributeBinding;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return pluralAttributeReference.getPersistentCollectionMetadata().getIndexDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMapKeyBinding( this );
	}

	@Override
	public String asLoggableText() {
		return "KEY(" + attributeBinding.asLoggableText() + ")";
	}


	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getPluralAttributeBinding();
	}

	@Override
	public String getUniqueIdentifier() {
		// for most index classifications, the uid should point to the "collection table"...
		return getPluralAttributeBinding().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		// for most index classifications, the "identification variable" (alias)
		// 		associated with the index should point to the identification variable
		// 		for the collection reference
		return getPluralAttributeBinding().getIdentificationVariable();
	}

	@Override
	public EntityTypeImplementor getIntrinsicSubclassEntityPersister() {
		// for most index classifications, there is none
		return null;
	}
}
