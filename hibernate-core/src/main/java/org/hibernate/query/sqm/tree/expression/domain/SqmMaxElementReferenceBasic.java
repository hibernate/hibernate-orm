/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmNavigable;

/**
 * @author Steve Ebersole
 */
public class SqmMaxElementReferenceBasic
		extends AbstractSpecificSqmElementReference
		implements SqmRestrictedCollectionElementReferenceBasic, SqmMaxElementReference {
	public SqmMaxElementReferenceBasic(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public SqmNavigable getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public SqmNavigable getInferableType() {
		return getReferencedNavigable();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMaxElementBinding( this );
	}

	@Override
	public String asLoggableText() {
		return "MAXELEMENT( " + getPluralAttributeBinding().asLoggableText() + ")";
	}

	@Override
	public SqmPluralAttributeReference getSourceReference() {
		return  getPluralAttributeBinding();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return getPluralAttributeBinding().getReferencedNavigable().getElementDescriptor();
	}
}
