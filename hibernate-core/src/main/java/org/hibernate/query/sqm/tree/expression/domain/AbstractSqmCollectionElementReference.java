/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.domain.SqmExpressableType;
import org.hibernate.query.sqm.domain.SqmPluralAttribute;
import org.hibernate.query.sqm.domain.type.SqmDomainType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCollectionElementReference extends AbstractSqmNavigableReference implements
		SqmCollectionElementReference {
	private final SqmPluralAttributeReference attributeBinding;
	private final SqmPluralAttribute pluralAttributeReference;
	private final NavigablePath propertyPath;

	public AbstractSqmCollectionElementReference(SqmPluralAttributeReference pluralAttributeBinding) {
		this.attributeBinding = pluralAttributeBinding;
		this.pluralAttributeReference = pluralAttributeBinding.getReferencedNavigable();

		this.propertyPath = pluralAttributeBinding.getNavigablePath().append( "{elements}" );
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return getPluralAttributeBinding();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeBinding().getReferencedNavigable().getElementDescriptor();
	}

	@Override
	public SqmDomainType getExportedDomainType() {
		return getReferencedNavigable().getExportedDomainType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public String asLoggableText() {
		return getNavigablePath().getFullPath();
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return getPluralAttributeBinding().getReferencedNavigable();
	}

	@Override
	public SqmExpressableType getInferableType() {
		return getExpressionType();
	}
}
