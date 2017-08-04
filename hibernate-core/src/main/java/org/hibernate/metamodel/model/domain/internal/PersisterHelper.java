/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class PersisterHelper {

	public static CollectionClassification interpretCollectionClassification(Collection collectionBinding) {
		if ( collectionBinding instanceof Bag || collectionBinding instanceof IdentifierBag ) {
			return CollectionClassification.BAG;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.List ) {
			return CollectionClassification.LIST;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.Set ) {
			return CollectionClassification.SET;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.Map ) {
			return CollectionClassification.MAP;
		}

		final Class javaType = collectionBinding.getJavaTypeDescriptor().getJavaType();
		if ( Set.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.SET;
		}

		if ( Map.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.MAP;
		}

		if ( List.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.LIST;
		}

		return CollectionClassification.BAG;
	}

	public static PropertyAccess resolvePropertyAccess(
			ManagedTypeDescriptor declarer,
			PersistentAttributeMapping attributeMapping,
			RuntimeModelCreationContext persisterCreationContext) {
		if ( declarer.geEntityMode() == EntityMode.MAP ) {
			return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess( null, attributeMapping.getName() );
		}

		final PropertyAccessStrategyResolver accessStrategyResolver = persisterCreationContext.getSessionFactory()
				.getServiceRegistry()
				.getService( PropertyAccessStrategyResolver.class );

		String propertyAccessorName = attributeMapping.getPropertyAccessorName();
		if ( propertyAccessorName == null ) {
			propertyAccessorName = "property";
		}

		final PropertyAccessStrategy propertyAccessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(
				declarer.getJavaType(),
				propertyAccessorName,
				declarer.getEntityMode()
		);

		return  propertyAccessStrategy.buildPropertyAccess( declarer.getJavaType(), attributeMapping.getName() );
	}

}
