/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Collection;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

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
	 * @param navigables
	 * @throws HibernateException
	 */
	void processValues(Object[] values, Collection<Navigable> navigables) throws HibernateException {
		int i = 0;
		for ( Navigable navigable : navigables ) {
			if ( includeProperty( values, i ) ) {
				processValue( values[i], navigable );
			}
			i++;
		}
	}
	
	/**
	 * Dispatch each property value to processValue().
	 *
	 * @param values
	 * @param navigables
	 * @throws HibernateException
	 */
	public void processEntityPropertyValues(Object[] values, List<Navigable> navigables) throws HibernateException {
		for ( int i = 0; i < navigables.size(); i++ ) {
			if ( includeEntityProperty( values, i ) ) {
				processValue( values[i], navigables.get( i ) );
			}
		}
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
	 * @param descriptor
	 * @throws HibernateException
	 */
	Object processComponent(Object component, EmbeddedTypeDescriptor descriptor) throws HibernateException {
		if ( component != null ) {
			processValues(
					descriptor.getPropertyValues( component ),
					descriptor.getSubclassTypes()
			);
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 * @param value
	 * @param navigable
	 * @throws HibernateException
	 */
	final Object processValue(Object value, Navigable navigable) throws HibernateException {

		if ( navigable instanceof PersistentCollectionDescriptor ) {
			return processCollection( value, (PersistentCollectionDescriptor) navigable );
		}
		if ( navigable instanceof EntityDescriptor ) {
			return processEntity( value, (EntityDescriptor) navigable );
		}
		else if ( navigable instanceof EmbeddedTypeDescriptor ) {
			return processComponent( value, (EmbeddedTypeDescriptor) navigable );
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
	void process(Object object, EntityDescriptor persister) throws HibernateException {
		processEntityPropertyValues(
				persister.getPropertyValues( object ),
				persister.getNavigables()
		);
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 * @param collection
	 * @param descriptor
	 * @throws HibernateException
	 */
	Object processCollection(Object collection, PersistentCollectionDescriptor descriptor) throws HibernateException {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 * @param value
	 * @param descriptor
	 * @throws HibernateException
	 */
	Object processEntity(Object value, EntityDescriptor descriptor) throws HibernateException {
		return null;
	}

	final EventSource getSession() {
		return session;
	}
}
