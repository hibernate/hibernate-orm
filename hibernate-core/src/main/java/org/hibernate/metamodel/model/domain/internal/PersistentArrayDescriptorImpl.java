/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.internal.PersistentArrayHolder;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
public class PersistentArrayDescriptorImpl extends AbstractPersistentCollectionDescriptor {

	public PersistentArrayDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext) {
		super( pluralProperty, runtimeContainer, CollectionClassification.ARRAY, creationContext );
	}

	@Override
	protected CollectionJavaDescriptor resolveCollectionJtd(RuntimeModelCreationContext creationContext) {
		Class componentType = getElementDescriptor().getJavaTypeDescriptor().getJavaType();
		if ( componentType == null ) {
			// MAP entity mode?
			// todo (6.0) : verify this
			componentType = Map.class;
		}

		// The only way I know to handle this is by instantiating an array...
		// todo (6.0) : better way?
		final Class arrayType = Array.newInstance( componentType, 0 ).getClass();
		assert arrayType.isArray();

		return findOrCreateCollectionJtd( arrayType, creationContext );
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return Array.newInstance(
				getJavaTypeDescriptor().getJavaType().getComponentType(),
				anticipatedSize
		);
	}

	@Override
	public PersistentCollection instantiateWrapper(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		return new PersistentArrayHolder( session, descriptor, key );
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object rawCollection) {
		return new PersistentArrayHolder( session, descriptor, rawCollection );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object collection, Object childObject) {
		assert collection.getClass().isArray();

		final int length = Array.getLength( collection );
		for ( int i = 0; i < length; i++ ) {
			if ( getElementDescriptor().getJavaTypeDescriptor().areEqual( Array.get( collection, i ), childObject ) ) {
				return true;
			}
		}

		return false;
	}


	@Override
	protected Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
