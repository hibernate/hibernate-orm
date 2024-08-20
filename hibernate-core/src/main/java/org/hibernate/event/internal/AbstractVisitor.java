/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Abstract superclass of algorithms that walk
 * a tree of property values of an entity, and
 * perform specific functionality for collections,
 * components and associated entities.
 *
 * @author Gavin King
 */
public abstract class AbstractVisitor {

	private final EventSource session;

	AbstractVisitor(EventSource session) {
		this.session = session;
	}

	/**
	 * Dispatch each property value to processValue().
	 *
	 */
	void processValues(Object[] values, Type[] types) throws HibernateException {
		for ( int i=0; i<types.length; i++ ) {
			if ( includeProperty(values, i) ) {
				processValue( i, values, types );
			}
		}
	}
	
	/**
	 * Dispatch each property value to processValue().
	 *
	 */
	public void processEntityPropertyValues(Object[] values, Type[] types) throws HibernateException {
		for ( int i=0; i<types.length; i++ ) {
			if ( includeEntityProperty(values, i) ) {
				processValue( i, values, types );
			}
		}
	}
	
	void processValue(int i, Object[] values, Type[] types) {
		processValue( values[i], types[i] );
	}
	
	boolean includeEntityProperty(Object[] values, int i) {
		return includeProperty(values, i);
	}
	
	boolean includeProperty(Object[] values, int i) {
		return values[i]!= LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	/**
	 * Visit a component. Dispatch each property
	 * to processValue().
	 */
	Object processComponent(Object component, CompositeType componentType) throws HibernateException {
		if (component!=null) {
			processValues(
				componentType.getPropertyValues(component, session),
				componentType.getSubtypes()
			);
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 */
	final Object processValue(Object value, Type type) throws HibernateException {

		if ( type instanceof CollectionType ) {
			//even process null collections
			return processCollection( value, (CollectionType) type );
		}
		else if ( type instanceof EntityType ) {
			return processEntity( value, (EntityType) type );
		}
		else if ( type instanceof ComponentType ) {
			return processComponent( value, (ComponentType) type );
		}
		else if ( type instanceof AnyType ) {
			return processComponent( value, (AnyType) type );
		}
		else {
			return null;
		}
	}

	/**
	 * Walk the tree starting from the given entity.
	 *
	 */
	public void process(Object object, EntityPersister persister)
	throws HibernateException {
		processEntityPropertyValues(
			persister.getValues( object ),
			persister.getPropertyTypes()
		);
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 */
	Object processCollection(Object collection, CollectionType type)
	throws HibernateException {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 */
	Object processEntity(Object value, EntityType entityType)
	throws HibernateException {
		return null;
	}

	protected final EventSource getSession() {
		return session;
	}
}
