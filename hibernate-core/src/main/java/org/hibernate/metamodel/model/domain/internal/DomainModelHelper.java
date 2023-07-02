/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;

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
			// todo : at least validate the string is a valid subtype of the embeddable class?
			return (ManagedDomainType<S>) baseType;
		}

		// first, try to find it by name directly
		ManagedDomainType<S> subManagedType = jpaMetamodel.resolveHqlEntityReference( subTypeName );
		if ( subManagedType != null ) {
			return subManagedType;
		}

		// it could still be a mapped-superclass
		try {
			final Class<?> javaType = jpaMetamodel.getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( subTypeName );
			return (ManagedDomainType<S>) jpaMetamodel.managedType( javaType );
		}
		catch (Exception ignore) {
		}

		throw new IllegalArgumentException( "Unknown subtype name (" + baseType.getTypeName() + ") : " + subTypeName );
	}

	static boolean isCompatible(
			PersistentAttribute<?, ?> attribute1,
			PersistentAttribute<?, ?> attribute2,
			JpaMetamodelImplementor jpaMetamodel) {
		if ( attribute1 == attribute2 ) {
			return true;
		}
		final MappingMetamodel mappingMetamodel = jpaMetamodel.getMappingMetamodel();
		final EntityMappingType entity1 = mappingMetamodel.getEntityDescriptor(
				attribute1.getDeclaringType().getTypeName()
		);
		final EntityMappingType entity2 = mappingMetamodel.getEntityDescriptor(
				attribute2.getDeclaringType().getTypeName()
		);

		return entity1 != null && entity2 != null && MappingModelHelper.isCompatibleModelPart(
				entity1.findSubPart( attribute1.getName() ),
				entity2.findSubPart( attribute2.getName() )
		);
	}

	public static <J, S> ManagedDomainType<S> findSubType(ManagedDomainType<J> type, Class<S> subtype) {
		if ( type.getBindableJavaType() == subtype ) {
			@SuppressWarnings("unchecked")
			final ManagedDomainType<S> result = (ManagedDomainType<S>) type;
			return result;
		}
		for ( ManagedDomainType<? extends J> candidate : type.getSubTypes() ) {
			if ( candidate.getBindableJavaType() == subtype ) {
				@SuppressWarnings("unchecked")
				final ManagedDomainType<S> result = (ManagedDomainType<S>) candidate;
				return result;
			}
		}
		for ( ManagedDomainType<? extends J> candidate : type.getSubTypes() ) {
			final ManagedDomainType<S> candidateSubtype = findSubType( candidate, subtype );
			if ( candidateSubtype != null) {
				return candidateSubtype;
			}
		}
		throw new IllegalArgumentException( "The class '" + subtype.getName()
				+ "' is not a mapped subtype of '" + type.getTypeName() + "'" );
//		return metamodel.managedType( subtype );
	}

	public static <J, S> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<J> type, Class<S> subtype) {
		if ( type.getBindableJavaType().isAssignableFrom( subtype ) ) {
			return new SubGraphImpl( type, true );
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Type '%s' cannot be treated as subtype '%s'",
							type.getTypeName(),
							subtype.getName()
					)
			);
		}
	}
}
