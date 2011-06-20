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
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
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
	 * @param values
	 * @param types
	 * @throws HibernateException
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
	 * @param values
	 * @param types
	 * @throws HibernateException
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
	 * @param component
	 * @param componentType
	 * @throws HibernateException
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
	 * @param value
	 * @param type
	 * @throws HibernateException
	 */
	final Object processValue(Object value, Type type) throws HibernateException {

		if ( type.isCollectionType() ) {
			//even process null collections
			return processCollection( value, (CollectionType) type );
		}
		else if ( type.isEntityType() ) {
			return processEntity( value, (EntityType) type );
		}
		else if ( type.isComponentType() ) {
			return processComponent( value, (CompositeType) type );
		}
		else {
			return null;
		}
	}

	/**
	 * Walk the tree starting from the given entity.
	 *
	 * @param object
	 * @param persister
	 * @throws HibernateException
	 */
	void process(Object object, EntityPersister persister)
	throws HibernateException {
		processEntityPropertyValues(
			persister.getPropertyValues( object ),
			persister.getPropertyTypes()
		);
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 * @param collection
	 * @param type
	 * @throws HibernateException
	 */
	Object processCollection(Object collection, CollectionType type)
	throws HibernateException {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 * @param value
	 * @param entityType
	 * @throws HibernateException
	 */
	Object processEntity(Object value, EntityType entityType)
	throws HibernateException {
		return null;
	}

	final EventSource getSession() {
		return session;
	}
}
