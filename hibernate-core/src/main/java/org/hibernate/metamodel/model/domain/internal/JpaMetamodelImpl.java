/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodel {
	private static final String INVALID_IMPORT = "";

	private final TypeConfiguration typeConfiguration;

	private final Map<String, EntityDomainType<?>> entityDescriptorMap;
	private final Map<Class, EntityDomainType<?>> strictEntityDescriptorMap;
	private final Map<Class, EmbeddableDomainType<?>> embeddableDescriptorMap;
	private final Map<Class,String> entityProxyInterfaceMap = new ConcurrentHashMap<>();

	private final Map<String,String> nameToImportNameMap = new ConcurrentHashMap<>();
	private final Map<Class, SqmPolymorphicRootDescriptor<?>> polymorphicEntityReferenceMap = new ConcurrentHashMap<>();

	public JpaMetamodelImpl(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		final String rename = resolveImportedName( entityName );
		if ( rename != null ) {
			entityName = rename;
		}

		final EntityDomainType<X> entityDescriptor = entity( entityName );
		if ( entityDescriptor != null ) {
			return entityDescriptor;
		}

		final Class<X> requestedClass = resolveRequestedClass( entityName );
		if ( requestedClass != null ) {
			return resolveEntityReference( requestedClass );
		}

		throw new IllegalArgumentException( "Could not resolve entity reference " + entityName );
	}

	private String resolveImportedName(String name) {
		String result = nameToImportNameMap.get( name );
		if ( result == null ) {
			// see if the name is a fully-qualified class name
			try {
				getServiceRegistry().getService( ClassLoaderService.class ).classForName( name );

				// it is a fully-qualified class name - add it to the cache
				//		so we do not keep trying later
				nameToImportNameMap.put( name, name );
				return name;
			}
			catch ( ClassLoadingException cnfe ) {
				// it is a NOT fully-qualified class name - add a marker entry
				//		so we do not keep trying later
				nameToImportNameMap.put( name, INVALID_IMPORT );
				return null;
			}
		}
		else {
			// explicitly check for same instance
			//noinspection StringEquality
			if ( result == INVALID_IMPORT ) {
				return null;
			}
			else {
				return result;
			}
		}
	}

	private <X> Class<X> resolveRequestedClass(String entityName) {
		try {
			return getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> EntityDomainType<T> resolveEntityReference(Class<T> javaType) {
		// try the incoming Java type as a "strict" entity reference
		{
			final EntityDomainType<?> descriptor = strictEntityDescriptorMap.get( javaType );
			if ( descriptor != null ) {
				return (EntityDomainType<T>) descriptor;
			}
		}

		// Next, try it as a proxy interface reference
		{
			final String proxyEntityName = entityProxyInterfaceMap.get( javaType );
			if ( proxyEntityName != null ) {
				return (EntityDomainType<T>) entityDescriptorMap.get( proxyEntityName );
			}
		}

		// otherwise, try to handle it as a polymorphic reference
		{
			if ( polymorphicEntityReferenceMap.containsKey( javaType ) ) {
				return (EntityDomainType<T>) polymorphicEntityReferenceMap.get( javaType );
			}

			final Set<EntityDomainType<?>> matchingDescriptors = new HashSet<>();
			visitEntityTypes(
					entityDomainType -> {
						if ( javaType.isAssignableFrom( entityDomainType.getJavaType() ) ) {
							matchingDescriptors.add( entityDomainType );
						}
					}
			);
			if ( !matchingDescriptors.isEmpty() ) {
				final SqmPolymorphicRootDescriptor descriptor = new SqmPolymorphicRootDescriptor(
						typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( javaType ),
						matchingDescriptors
				);
				polymorphicEntityReferenceMap.put( javaType, descriptor );
				return descriptor;
			}
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}
}
