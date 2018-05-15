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
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

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
	 * @param attributes
	 * @throws HibernateException
	 */
	final void processValues(Object[] values, Collection<PersistentAttributeDescriptor> attributes) throws HibernateException {
		int i = 0;
		for ( PersistentAttributeDescriptor attribute : attributes ) {
			if ( includeProperty( values, i ) ) {
				processValue( i, values, attribute );
			}
			i++;
		}
	}

	void processValue(int i, Object[] values, PersistentAttributeDescriptor attribute) throws HibernateException {
		processValue( values[i], attribute );
	}

	/**
	 * Dispatch each property value to processValue().
	 *
	 * @param values
	 * @param attributes
	 * @throws HibernateException
	 */
	public void processEntityPropertyValues(Object[] values, List<PersistentAttributeDescriptor> attributes) throws HibernateException {
		for ( int i = 0; i < attributes.size(); i++ ) {
			if ( includeEntityProperty( values, i ) ) {
				processValue(i, values, attributes.get( i ) );
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
					descriptor.getPersistentAttributes()
			);
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 * @param value
	 * @param attribute
	 * @throws HibernateException
	 */
	final Object processValue(Object value, PersistentAttributeDescriptor attribute) throws HibernateException {

		if ( attribute instanceof PluralPersistentAttribute ) {
			return processCollection( value, (PluralPersistentAttribute) attribute );
		}
		if ( attribute instanceof EntityTypeDescriptor ) {
			return processEntity( value, (EntityTypeDescriptor) attribute );
		}
		else if ( attribute instanceof EmbeddedTypeDescriptor ) {
			return processComponent( value, (EmbeddedTypeDescriptor) attribute );
		}
		else {
			return null;
		}
	}

	/**
	 * Walk the tree starting from the given entity.
	 *
	 * @param object
	 * @param entityDescriptor
	 * @throws HibernateException
	 */
	void process(Object object, EntityTypeDescriptor entityDescriptor) throws HibernateException {
		processEntityPropertyValues(
				entityDescriptor.getPropertyValues( object ),
				entityDescriptor.getPersistentAttributes()
		);
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 * @param collection
	 * @param collectionAttribute
	 * @throws HibernateException
	 */
	Object processCollection(Object collection, PluralPersistentAttribute collectionAttribute) throws HibernateException {
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
	Object processEntity(Object value, EntityTypeDescriptor descriptor) throws HibernateException {
		return null;
	}

	final EventSource getSession() {
		return session;
	}
}
