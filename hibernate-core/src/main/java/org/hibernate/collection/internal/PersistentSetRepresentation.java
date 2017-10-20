/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.HashMap;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentCollectionRepresentation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class PersistentSetRepresentation implements PersistentCollectionRepresentation {
	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SET;
	}

	@Override
	public Class getPersistentCollectionJavaType() {
		return PersistentSet.class;
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return new HashMap<>(
				CollectionHelper.determineProperSizing( anticipatedSize )
		);
	}

	@Override
	public PersistentCollection instantiateWrapped(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		return new PersistentSet( session, descriptor, key );
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session, PersistentCollectionDescriptor descriptor, Object rawCollection) {
		return null;
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		return false;
	}

	@Override
	public PersistentCollectionDescriptor generatePersistentCollectionDescriptor(
			ManagedTypeDescriptor runtimeContainer,
			ManagedTypeMapping bootContainer,
			Property bootProperty,
			RuntimeModelCreationContext context) {
		return null;
	}
}
