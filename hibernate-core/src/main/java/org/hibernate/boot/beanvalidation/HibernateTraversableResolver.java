/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import jakarta.validation.Path;
import jakarta.validation.TraversableResolver;

/**
 * Use Hibernate metadata to ignore cascade on entities.
 * Cascade on embeddable objects or collection of embeddable objects are accepted
 * Also use Hibernate's native {@link Hibernate#isInitialized} method call.
 *
 * @author Emmanuel Bernard
 */
public class HibernateTraversableResolver implements TraversableResolver {
	private final Map<Class<?>, Set<String>> associationsPerEntityClass = new HashMap<>();

	public void addPersister(EntityPersister persister, SessionFactoryImplementor factory) {
		final var javaTypeClass = persister.getEntityMappingType().getMappedJavaType().getJavaTypeClass();
		final Set<String> associations = new HashSet<>();
		addAssociationsToTheSetForAllProperties( persister.getPropertyNames(), persister.getPropertyTypes(), "", factory, associations );
		associationsPerEntityClass.put( javaTypeClass, associations );
	}

	private static void addAssociationsToTheSetForAllProperties(
			String[] names, Type[] types, String prefix, SessionFactoryImplementor factory, Set<String> associations) {
		final int length = names.length;
		for( int index = 0 ; index < length; index++ ) {
			addAssociationsToTheSetForOneProperty( names[index], types[index], prefix, factory, associations );
		}
	}

	private static void addAssociationsToTheSetForOneProperty(
			String name, Type type, String prefix, SessionFactoryImplementor factory, Set<String> associations) {
		if ( type instanceof CollectionType collectionType ) {
			addAssociationsToTheSetForOneProperty( name, collectionType.getElementType( factory ), prefix, factory, associations );
		}
		//ToOne association
		else if ( type instanceof EntityType || type instanceof AnyType ) {
			associations.add( prefix + name );
		}
		else if ( type instanceof ComponentType componentType ) {
			addAssociationsToTheSetForAllProperties(
					componentType.getPropertyNames(),
					componentType.getSubtypes(),
					( prefix.isEmpty() ? name : prefix + name ) + '.',
					factory,
					associations
			);
		}
	}

	private String getStringBasedPath(Path.Node traversableProperty, Path pathToTraversableObject) {
		final var path = new StringBuilder( );
		for ( var node : pathToTraversableObject ) {
			if (node.getName() != null) {
				path.append( node.getName() ).append( '.' );
			}
		}
		if ( traversableProperty.getName() == null ) {
			throw new AssertionFailure(
					"TraversableResolver being passed a traversableProperty with null name. pathToTraversableObject: "
							+ path );
		}
		path.append( traversableProperty.getName() );
		return path.toString();
	}

	public boolean isReachable(Object traversableObject,
			Path.Node traversableProperty,
			Class<?> rootBeanType,
			Path pathToTraversableObject,
			ElementType elementType) {
		//lazy, don't load
		return Hibernate.isInitialized( traversableObject )
			&& Hibernate.isPropertyInitialized( traversableObject, traversableProperty.getName() );
	}

	public boolean isCascadable(Object traversableObject,
			Path.Node traversableProperty,
			Class<?> rootBeanType,
			Path pathToTraversableObject,
			ElementType elementType) {
		final var associations = associationsPerEntityClass.get( rootBeanType);
		return associations != null
			&& !associations.contains( getStringBasedPath( traversableProperty, pathToTraversableObject ) );
	}
}
