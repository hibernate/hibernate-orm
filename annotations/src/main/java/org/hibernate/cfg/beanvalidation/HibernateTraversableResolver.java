package org.hibernate.cfg.beanvalidation;

import java.lang.annotation.ElementType;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.TraversableResolver;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.Hibernate;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.AbstractComponentType;

/**
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
			AbstractComponentType componentType = (AbstractComponentType) type;
			addAssociationsToTheSetForAllProperties(
					componentType.getPropertyNames(),
					componentType.getSubtypes(),
					(prefix.equals( "" ) ? name : prefix + name) + ".",
					factory);
		}
	}

	private String getCleanPathWoBraket(String traversableProperty, String pathToTraversableObject) {
		String path = pathToTraversableObject.equals( "" ) ?
				traversableProperty :
				pathToTraversableObject + "." + traversableProperty;
		String[] paths = path.split( "\\[.*\\]" );
		path = "";
		for (String subpath : paths) {
			path += subpath;
		}
		return path;
	}

	public boolean isReachable(Object traversableObject,
						  String traversableProperty,
						  Class<?> rootBeanType,
						  String pathToTraversableObject,
						  ElementType elementType) {
		//lazy, don't load
		return Hibernate.isInitialized( traversableObject )
				&& Hibernate.isPropertyInitialized( traversableObject, traversableProperty );
	}

	public boolean isCascadable(Object traversableObject,
						  String traversableProperty,
						  Class<?> rootBeanType,
						  String pathToTraversableObject,
						  ElementType elementType) {
		String path = getCleanPathWoBraket( traversableProperty, pathToTraversableObject );
		return ! associations.contains(path);
	}
}
