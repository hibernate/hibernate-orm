/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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
		return getPluralAttributeBinding();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeBinding().getReferencedNavigable().getElementDescriptor();
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public SqmExpressableType getInferableType() {
		return getExpressionType();
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
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}
}
