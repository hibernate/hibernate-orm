/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.beanvalidation;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
	private Set<String> associations;

	public HibernateTraversableResolver(
			EntityPersister persister,
			ConcurrentHashMap<EntityPersister, Set<String>> associationsPerEntityPersister,
			SessionFactoryImplementor factory) {
		associations = associationsPerEntityPersister.get( persister );
		if ( associations == null ) {
			associations = new HashSet<>();
			addAssociationsToTheSetForAllProperties( persister.getPropertyNames(), persister.getPropertyTypes(), "", factory );
			associationsPerEntityPersister.put( persister, associations );
		}
	}

	private void addAssociationsToTheSetForAllProperties(
			String[] names, Type[] types, String prefix, SessionFactoryImplementor factory) {
		final int length = names.length;
		for( int index = 0 ; index < length; index++ ) {
			addAssociationsToTheSetForOneProperty( names[index], types[index], prefix, factory );
		}
	}

	private void addAssociationsToTheSetForOneProperty(
			String name, Type type, String prefix, SessionFactoryImplementor factory) {
		if ( type instanceof CollectionType collectionType ) {
			addAssociationsToTheSetForOneProperty( name, collectionType.getElementType( factory ), prefix, factory );
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
					factory
			);
		}
	}

	private String getStringBasedPath(Path.Node traversableProperty, Path pathToTraversableObject) {
		final StringBuilder path = new StringBuilder( );
		for ( Path.Node node : pathToTraversableObject ) {
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
		return !associations.contains( getStringBasedPath( traversableProperty, pathToTraversableObject ) );
	}
}
