/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class PersistentMapDescriptorImpl extends AbstractPersistentCollectionDescriptor {
	public PersistentMapDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext)
			throws MappingException, CacheException {
		super( pluralProperty, runtimeContainer, CollectionClassification.MAP, creationContext );
	}

	@Override
	protected CollectionJavaDescriptor resolveCollectionJtd(RuntimeModelCreationContext creationContext) {
		return findCollectionJtd( Map.class, creationContext );
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return CollectionHelper.mapOfSize( anticipatedSize );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		return new PersistentMap( session, descriptor, key );
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object rawCollection) {
		return new PersistentMap( session, descriptor, (Map) rawCollection );
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		// todo (6.0) : do we need to check key as well?
		// todo (6.0) : or perhaps make distinction between #containsValue and #containsKey/Index?
		return ( (Map) collection ).containsValue( childObject );
	}

	@Override
	protected Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
