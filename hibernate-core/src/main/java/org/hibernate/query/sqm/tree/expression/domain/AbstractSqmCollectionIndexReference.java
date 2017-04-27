/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.collection.spi.CollectionPersister.CollectionClassification;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.PluralPersistentAttribute;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCollectionIndexReference extends AbstractSqmNavigableReference implements
		SqmCollectionIndexReference {
	private final SqmPluralAttributeReference attributeBinding;
	private final PluralPersistentAttribute pluralAttributeReference;
	private final NavigablePath propertyPath;

	public AbstractSqmCollectionIndexReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeBinding = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		assert pluralAttributeReference.getCollectionPersister().getCollectionClassification() == CollectionClassification.MAP
				|| pluralAttributeReference.getCollectionPersister().getCollectionClassification() == CollectionClassification.LIST;

		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{keys}" );
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public ExpressableType getExpressionType() {
		return getPluralAttributeBinding().getReferencedNavigable().getCollectionPersister().getIndexDescriptor();
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
		return pluralAttributeReference.getCollectionPersister().getIndexDescriptor();
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
}
