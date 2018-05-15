/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.BasicCollectionIndex;

/**
 * @author Steve Ebersole
 */
public class SqmMaxIndexReferenceBasic extends AbstractSpecificSqmCollectionIndexReference
		implements SqmMaxIndexReference {
	public SqmMaxIndexReferenceBasic(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public BasicCollectionIndex getReferencedNavigable() {
		return (BasicCollectionIndex) super.getReferencedNavigable();
	}
}
