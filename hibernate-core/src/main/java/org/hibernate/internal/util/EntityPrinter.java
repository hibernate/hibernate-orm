/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Renders entities and query parameters to a nicely readable string.
 *
 * @author Gavin King
 */
public final class EntityPrinter {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityPrinter.class );

	private final SessionFactoryImplementor factory;

	/**
	 * Renders an entity to a string.
	 *
	 * @param entityName the entity name
	 * @param entity an actual entity object, not a proxy!
	 *
	 * @return the entity rendered to a string
	 */
	public String toString(String entityName, Object entity) throws HibernateException {
		final EntityPersister entityPersister =
				factory.getMappingMetamodel()
						.getEntityDescriptor( entityName );
		if ( entityPersister == null || !entityPersister.isInstance( entity ) ) {
			return entity.getClass().getName();
		}
		else {
			final Map<String, String> result = new HashMap<>();
			if ( entityPersister.hasIdentifierProperty() ) {
				result.put(
						entityPersister.getIdentifierPropertyName(),
						entityPersister.getIdentifierType()
								.toLoggableString( entityPersister.getIdentifier( entity ), factory )
				);
			}
			final Type[] types = entityPersister.getPropertyTypes();
			final String[] names = entityPersister.getPropertyNames();
			final Object[] values = entityPersister.getValues( entity );
			for ( int i = 0; i < types.length; i++ ) {
				if ( !names[i].startsWith( "_" ) ) {
					final String strValue;
					if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
						strValue = values[i].toString();
					}
					else if ( !Hibernate.isInitialized( values[i] ) ) {
						strValue = "<uninitialized>";
					}
					else {
						strValue = types[i].toLoggableString( values[i], factory );
					}
					result.put( names[i], strValue );
				}
			}
			return entityName + result;
		}
	}

	public String toString(Type[] types, Object[] values) throws HibernateException {
		final StringBuilder buffer = new StringBuilder();
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i] != null ) {
				buffer.append( types[i].toLoggableString( values[i], factory ) ).append( ", " );
			}
		}
		return buffer.toString();
	}

	public String toString(Map<String, TypedValue> namedTypedValues) throws HibernateException {
		final Map<String, String> result = new HashMap<>();
		for ( Map.Entry<String, TypedValue> entry : namedTypedValues.entrySet() ) {
			final String key = entry.getKey();
			final TypedValue value = entry.getValue();
			result.put( key, value.getType().toLoggableString( value.getValue(), factory ) );
		}
		return result.toString();
	}

	// Cannot use Map as an argument because it clashes with the previous method (due to type erasure)
	public void logEntities(Iterable<Map.Entry<EntityKey, EntityHolder>> entitiesByEntityKey)
			throws HibernateException {
		if ( LOG.isDebugEnabled() && entitiesByEntityKey.iterator().hasNext() ) {
			LOG.debug( "Listing entities:" );
			int i = 0;
			for ( Map.Entry<EntityKey, EntityHolder> entityKeyAndEntity : entitiesByEntityKey ) {
				final EntityHolder holder = entityKeyAndEntity.getValue();
				if ( holder.getEntity() == null ) {
					continue;
				}
				if ( i++ > 20 ) {
					LOG.debug( "More......" );
					break;
				}
				LOG.debug( toString( entityKeyAndEntity.getKey().getEntityName(), holder.getEntity() ) );
			}
		}
	}

	public EntityPrinter(SessionFactoryImplementor factory) {
		this.factory = factory;
	}
}
