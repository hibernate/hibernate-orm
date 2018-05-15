/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Array;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PersistentArrayDescriptorImpl<O,E> extends AbstractPersistentCollectionDescriptor<O,E[], E> {

	public PersistentArrayDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext) {
		super( pluralProperty, runtimeContainer, creationContext );
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	protected CollectionJavaDescriptor resolveCollectionJtd(
//			Collection collectionBinding,
//			RuntimeModelCreationContext creationContext) {
//		Class componentType = getElementDescriptor().getJavaTypeDescriptor().getJavaType();
//		if ( componentType == null ) {
//			// MAP entity mode?
//			// todo (6.0) : verify this
//			componentType = Map.class;
//		}
//
//		// The only way I know to handle this is by instantiating an array...
//		// todo (6.0) : better way?
//		final Class arrayType = Array.newInstance( componentType, 0 ).getClass();
//		assert arrayType.isArray();
//
//		final CollectionJavaDescriptor javaDescriptor = new CollectionJavaDescriptor(
//				arrayType,
//				StandardArraySemantics.INSTANCE
//		);
//		creationContext.getTypeConfiguration().getJavaTypeDescriptorRegistry().addDescriptor( javaDescriptor );
//
//		return javaDescriptor;
//	}

	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	protected AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		return new ListAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
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

}
