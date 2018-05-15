/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.type.Type;

/**
 * Renders entities and query parameters to a nicely readable string.
 *
 * @author Gavin King
 */
public final class EntityPrinter {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityPrinter.class );

	private SessionFactoryImplementor factory;

	/**
	 * Renders an entity to a string.
	 *
	 * @param entityName the entity name
	 * @param entity an actual entity object, not a proxy!
	 * @param session the session
	 *
	 * @return the entity rendered to a string
	 */
	public String toString(
			String entityName,
			Object entity,
			SharedSessionContractImplementor session) throws HibernateException {
		EntityTypeDescriptor entityDescriptor = factory.getEntityPersister( entityName );

		if ( entityDescriptor == null || !entityDescriptor.isInstance( entity ) ) {
			return entity.getClass().getName();
		}

		Map<String, String> result = new HashMap<String, String>();

		if ( entityDescriptor.getIdentifierDescriptor() != null ) {
			result.put(
					entityDescriptor.getIdentifierPropertyName(),
					entityDescriptor.getIdentifierDescriptor().getJavaTypeDescriptor().extractLoggableRepresentation(
							entityDescriptor.getIdentifierDescriptor().extractIdentifier( entity, session )
					)
			);
		}

		Type[] types = entityDescriptor.getPropertyTypes();
		String[] names = entityDescriptor.getPropertyNames();
		Object[] values = entityDescriptor.getPropertyValues( entity );
		for ( int i = 0; i < types.length; i++ ) {
			if ( !names[i].startsWith( "_" ) ) {
				String strValue = values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ?
						values[i].toString() :
						types[i].toLoggableString( values[i] );
				result.put( names[i], strValue );
			}
		}
		return entityName + result.toString();
	}

	public String toString(Type[] types, Object[] values) throws HibernateException {
		StringBuilder buffer = new StringBuilder();
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i] != null ) {
				buffer.append( types[i].toLoggableString( values[i] ) ).append( ", " );
			}
		}
		return buffer.toString();
	}

	public String toString(Map<String, TypedValue> namedTypedValues) throws HibernateException {
		Map<String, String> result = new HashMap<String, String>();
		for ( Map.Entry<String, TypedValue> entry : namedTypedValues.entrySet() ) {
			result.put(
					entry.getKey(), entry.getValue().getType().toLoggableString(
							entry.getValue().getValue()
					)
			);
		}
		return result.toString();
	}

	// Cannot use Map as an argument because it clashes with the previous method (due to type erasure)
	public void toString(
			Iterable<Map.Entry<EntityKey, Object>> entitiesByEntityKey,
			SharedSessionContractImplementor session) throws HibernateException {
		if ( !LOG.isDebugEnabled() || !entitiesByEntityKey.iterator().hasNext() ) {
			return;
		}

		LOG.debug( "Listing entities:" );
		int i = 0;
		for ( Map.Entry<EntityKey, Object> entityKeyAndEntity : entitiesByEntityKey ) {
			if ( i++ > 20 ) {
				LOG.debug( "More......" );
				break;
			}
			LOG.debug( toString( entityKeyAndEntity.getKey().getEntityName(), entityKeyAndEntity.getValue(), session ) );
		}
	}

	public EntityPrinter(SessionFactoryImplementor factory) {
		this.factory = factory;
	}
}
