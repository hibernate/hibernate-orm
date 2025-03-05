/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		return includeProperty( values, i );
	}

	boolean includeProperty(Object[] values, int i) {
		return values[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	/**
	 * Visit a component. Dispatch each property
	 * to processValue().
	 */
	Object processComponent(Object component, CompositeType componentType) throws HibernateException {
		if ( component != null ) {
			processValues( componentType.getPropertyValues(component, session), componentType.getSubtypes() );
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 */
	final Object processValue(Object value, Type type) throws HibernateException {
		if ( type instanceof CollectionType collectionType ) {
			//even process null collections
			return processCollection( value, collectionType );
		}
		else if ( type instanceof EntityType entityType ) {
			return processEntity( value, entityType );
		}
		else if ( type instanceof ComponentType componentType ) {
			return processComponent( value, componentType );
		}
		else if ( type instanceof AnyType anyType ) {
			return processComponent( value, anyType );
		}
		else {
			return null;
		}
	}

	/**
	 * Walk the tree starting from the given entity.
	 *
	 */
	public void process(Object object, EntityPersister persister) throws HibernateException {
		processEntityPropertyValues( persister.getValues( object ), persister.getPropertyTypes() );
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 */
	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 */
	Object processEntity(Object value, EntityType entityType) throws HibernateException {
		return null;
	}

	protected final EventSource getSession() {
		return session;
	}
}
