/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
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

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractVisitor.class );

	private final EventSource session;

	AbstractVisitor(EventSource session) {
		this.session = session;
	}

	/**
	 * Dispatch each property value to processValue().
	 *
	 */
	void processValues(Object entity, Object[] values, Type[] types) throws HibernateException {
		for ( int i=0; i<types.length; i++ ) {
			if ( includeProperty(values, i) ) {
				processValue( entity, i, values, types );
			}
		}
	}

	/**
	 * Dispatch each property value to processValue().
	 *
	 */
	public void processEntityPropertyValues(Object entity, Object[] values, Type[] types) throws HibernateException {
		for ( int i=0; i<types.length; i++ ) {
			boolean includeEntityProperty = includeEntityProperty(values, i);
			if (LOG.isTraceEnabled()) {
				LOG.trace( "processEntityPropertyValues: type=" + types[i] + ", includeEntityProperty=" + includeEntityProperty );
			}
			if ( includeEntityProperty ) {
				processValue( entity, i, values, types );
			}
		}
	}

	void processValue(Object entity, int i, Object[] values, Type[] types) {
		processValue( entity, values[i], types[i], i );
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
	Object processComponent(Object entity, Object component, CompositeType componentType) throws HibernateException {
		if ( component != null ) {
			processValues( entity, componentType.getPropertyValues(component, session), componentType.getSubtypes() );
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 */
	final Object processValue(Object entity, Object value, Type type, int index) throws HibernateException {
		Object rValue = null;
		if ( type instanceof CollectionType collectionType ) {
			//even process null collections
			rValue = processCollection( entity, value, collectionType );
		}
		else if ( type instanceof EntityType entityType ) {
			rValue = processEntity( entity, value, entityType );
		}
		else if ( type instanceof ComponentType componentType ) {
			rValue = processComponent( entity, value, componentType );
		}
		else if ( type instanceof AnyType anyType ) {
			rValue = processComponent( entity, value, anyType );
		}
		if (LOG.isTraceEnabled()) {
			LOG.trace( "processValue: rValue=" + rValue + ", index=" + index + ", entity=" + entity + ", value=" + value + ", type=" + type );
		}
		return rValue;
	}

	/**
	 * Walk the tree starting from the given entity.
	 *
	 */
	public void process(Object object, EntityPersister persister) throws HibernateException {
		processEntityPropertyValues( object, persister.getValues( object ), persister.getPropertyTypes() );
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 */
	Object processCollection(Object entity, Object collection, CollectionType type) throws HibernateException {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 */
	Object processEntity(Object entity, Object value, EntityType entityType) throws HibernateException {
		return null;
	}

	protected final EventSource getSession() {
		return session;
	}
}
