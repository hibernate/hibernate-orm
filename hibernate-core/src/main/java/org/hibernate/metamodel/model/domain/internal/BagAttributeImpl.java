/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;

/**
 * @author Steve Ebersole
 */
class BagAttributeImpl<X, E> extends AbstractPluralAttribute<X, Collection<E>, E>
		implements BagPersistentAttribute<X, E> {
	BagAttributeImpl(PluralAttributeBuilder<X, Collection<E>, E, ?> xceBuilder) {
		super( xceBuilder );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.COLLECTION;
	}
}
