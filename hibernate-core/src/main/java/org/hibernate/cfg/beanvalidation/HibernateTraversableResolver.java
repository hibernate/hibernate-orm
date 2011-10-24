/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.beanvalidation;
import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Use Hibernate metadata to ignore cascade on entities.
 * cascade on embeddable objects or collection of embeddable objects are accepted
 *
 * Also use Hibernate's native isInitialized method call.
 * 
 * @author Emmanuel Bernard
 */
public class HibernateTraversableResolver implements TraversableResolver {
	private Set<String> associations;

	public HibernateTraversableResolver(
			EntityPersister persister,
			ConcurrentHashMap<EntityPersister, Set<String>> associationsPerEntityPersister, 
			SessionFactoryImplementor factory) {
		this.associations = associationsPerEntityPersister.get( persister );
		if (this.associations == null) {
			this.associations = new HashSet<String>();
			addAssociationsToTheSetForAllProperties( persister.getPropertyNames(), persister.getPropertyTypes(), "", factory );
			associationsPerEntityPersister.put( persister, associations );
		}
	}

	private void addAssociationsToTheSetForAllProperties(String[] names, Type[] types, String prefix, SessionFactoryImplementor factory) {
		final int length = names.length;
		for( int index = 0 ; index < length; index++ ) {
			addAssociationsToTheSetForOneProperty( names[index], types[index], prefix, factory );
		}
	}

	private void addAssociationsToTheSetForOneProperty(String name, Type type, String prefix, SessionFactoryImplementor factory) {

		if ( type.isCollectionType() ) {
			CollectionType collType = (CollectionType) type;
			Type assocType = collType.getElementType( factory );
			addAssociationsToTheSetForOneProperty(name, assocType, prefix, factory);
		}
		//ToOne association
		else if ( type.isEntityType() || type.isAnyType() ) {
			associations.add( prefix + name );
		} else if ( type.isComponentType() ) {
			CompositeType componentType = (CompositeType) type;
			addAssociationsToTheSetForAllProperties(
					componentType.getPropertyNames(),
					componentType.getSubtypes(),
					(prefix.equals( "" ) ? name : prefix + name) + ".",
					factory);
		}
	}

	private String getStringBasedPath(Path.Node traversableProperty, Path pathToTraversableObject) {
		StringBuilder path = new StringBuilder( );
		for ( Path.Node node : pathToTraversableObject ) {
			if (node.getName() != null) {
				path.append( node.getName() ).append( "." );
			}
		}
		if ( traversableProperty.getName() == null ) {
			throw new AssertionFailure(
					"TraversableResolver being passed a traversableProperty with null name. pathToTraversableObject: "
							+ path.toString() );
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
		String path = getStringBasedPath( traversableProperty, pathToTraversableObject );
		return ! associations.contains(path);
	}
}
