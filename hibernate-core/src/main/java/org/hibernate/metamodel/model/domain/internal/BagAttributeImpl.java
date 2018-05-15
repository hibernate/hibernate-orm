/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public class BagAttributeImpl<X, E> extends AbstractPluralPersistentAttribute<X, Collection<E>, E>
		implements BagPersistentAttribute<X, E> {

	public BagAttributeImpl(
			PersistentCollectionDescriptor collectionDescriptor,
			Property bootProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		super( collectionDescriptor, bootProperty, propertyAccess, creationContext );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.COLLECTION;
	}
}
