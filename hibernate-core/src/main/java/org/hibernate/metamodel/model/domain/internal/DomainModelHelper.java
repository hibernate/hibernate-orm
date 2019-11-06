/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Helper containing utilities useful for domain model handling

 * @author Steve Ebersole
 */
public class DomainModelHelper {

	@SuppressWarnings("unchecked")
	public static <T, S extends T> ManagedDomainType<S> resolveSubType(
			ManagedDomainType<T> baseType,
			String subTypeName,
			JpaMetamodel jpaMetamodel) {
		if ( baseType instanceof EmbeddableDomainType<?> ) {
			// todo : at least validate the string is a valid sub-type of the embeddable class?
			return (ManagedDomainType) baseType;
		}

		// first, try to find it by name directly..
		ManagedDomainType<S> subManagedType = jpaMetamodel.entity( subTypeName );
		if ( subManagedType != null ) {
			return subManagedType;
		}

		// it could still be a mapped-superclass
		try {
			final Class javaType = jpaMetamodel.getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( subTypeName );
			return jpaMetamodel.managedType( javaType );
		}
		catch (Exception ignore) {
		}

		throw new IllegalArgumentException( "Unknown sub-type name (" + baseType.getTypeName() + ") : " + subTypeName );
	}

	public static <S> ManagedDomainType<S> resolveSubType(
			ManagedDomainType<? super S> baseType,
			Class<S> subTypeClass,
			JpaMetamodel jpaMetamodel) {
		// todo : validate the hierarchy-ness...
		return jpaMetamodel.managedType( subTypeClass );
	}


}
