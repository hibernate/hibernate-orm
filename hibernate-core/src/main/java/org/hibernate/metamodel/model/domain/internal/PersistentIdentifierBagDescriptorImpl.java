/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Andrea Boriero
 */
public class PersistentIdentifierBagDescriptorImpl<O, E> extends PersistentBagDescriptorImpl<O, E> {

	public PersistentIdentifierBagDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext)
			throws MappingException, CacheException {
		super( pluralProperty, runtimeContainer, creationContext );
	}

	@Override
	protected AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		return new IdentifierBagAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
	}

	@Override
	protected void doProcessQueuedOps(
			PersistentCollection collection, Object id, SharedSessionContractImplementor session) {
//		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CollectionSemantics<Collection<E>> getSemantics() {
		return StandardIdentifierBagSemantics.INSTANCE;
	}
}
