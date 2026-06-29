/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

	AbstractVisitor(@Nonnull EventSource session) {
		this.session = session;
	}

	/**
	 * Dispatch each property value to processValue().
	 *
	 */
	void processValues(@Nonnull Object[] values, @Nonnull Type[] types) {
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
	public void processEntityPropertyValues(@Nonnull Object[] values, @Nonnull Type[] types) {
		for ( int i=0; i<types.length; i++ ) {
			if ( includeEntityProperty(values, i) ) {
				processValue( i, values, types );
			}
		}
	}

	void processValue(int i, @Nonnull Object[] values, @Nonnull Type[] types) {
		processValue( values[i], types[i] );
	}

	boolean includeEntityProperty(@Nonnull Object[] values, int i) {
		return includeProperty( values, i );
	}

	boolean includeProperty(@Nonnull Object[] values, int i) {
		return values[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	/**
	 * Visit a component. Dispatch each property
	 * to processValue().
	 */
	@Nullable
	Object processComponent(@Nullable Object component, @Nonnull CompositeType componentType) {
		if ( component != null ) {
			processValues( componentType.getPropertyValues(component, session), componentType.getSubtypes() );
		}
		return null;
	}

	/**
	 * Visit a property value. Dispatch to the
	 * correct handler for the property type.
	 */
	@Nullable
	final Object processValue(@Nullable Object value, @Nonnull Type type) {
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
	public void process(@Nonnull Object object, @Nonnull EntityPersister persister) {
		processEntityPropertyValues( persister.getValues( object ), persister.getPropertyTypes() );
	}

	/**
	 * Visit a collection. Default superclass
	 * implementation is a no-op.
	 */
	@Nullable Object processCollection(@Nullable Object collection, @Nonnull CollectionType type) {
		return null;
	}

	/**
	 * Visit a many-to-one or one-to-one associated
	 * entity. Default superclass implementation is
	 * a no-op.
	 */
	@Nullable Object processEntity(@Nullable Object value, @Nonnull EntityType entityType) {
		return null;
	}

	protected final @Nonnull EventSource getSession() {
		return session;
	}
}
