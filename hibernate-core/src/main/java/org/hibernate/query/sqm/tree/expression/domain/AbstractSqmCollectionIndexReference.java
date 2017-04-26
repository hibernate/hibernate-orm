/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableType;
import org.hibernate.query.sqm.domain.SqmPluralAttribute;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCollectionIndexReference extends AbstractSqmNavigableReference implements
		SqmCollectionIndexReference {
	private final SqmPluralAttributeReference attributeBinding;
	private final SqmPluralAttribute pluralAttributeReference;
	private final NavigablePath propertyPath;

	public AbstractSqmCollectionIndexReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeBinding = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		assert pluralAttributeReference.getCollectionClassification() == SqmPluralAttribute.CollectionClassification.MAP
				|| pluralAttributeReference.getCollectionClassification() == SqmPluralAttribute.CollectionClassification.LIST;

		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{keys}" );
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return getPluralAttributeBinding().getReferencedNavigable().getIndexDescriptor();
	}

	@Override
	public SqmExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return attributeBinding;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return pluralAttributeReference.getIndexDescriptor();
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
