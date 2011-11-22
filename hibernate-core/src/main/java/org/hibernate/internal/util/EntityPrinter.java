/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Renders entities and query parameters to a nicely readable string.
 * @author Gavin King
 */
public final class EntityPrinter {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityPrinter.class.getName());

    private SessionFactoryImplementor factory;

	/**
	 * Renders an entity to a string.
	 *
	 * @param entityName the entity name
	 * @param entity an actual entity object, not a proxy!
	 * @return the entity rendered to a string
	 */
	public String toString(String entityName, Object entity) throws HibernateException {
		EntityPersister entityPersister = factory.getEntityPersister( entityName );

		if ( entityPersister == null ) {
			return entity.getClass().getName();
		}

		Map<String,String> result = new HashMap<String,String>();

		if ( entityPersister.hasIdentifierProperty() ) {
			result.put(
				entityPersister.getIdentifierPropertyName(),
				entityPersister.getIdentifierType().toLoggableString( entityPersister.getIdentifier( entity ), factory )
			);
		}

		Type[] types = entityPersister.getPropertyTypes();
		String[] names = entityPersister.getPropertyNames();
		Object[] values = entityPersister.getPropertyValues( entity );
		for ( int i=0; i<types.length; i++ ) {
			if ( !names[i].startsWith("_") ) {
				String strValue = values[i]==LazyPropertyInitializer.UNFETCHED_PROPERTY ?
					values[i].toString() :
					types[i].toLoggableString( values[i], factory );
				result.put( names[i], strValue );
			}
		}
		return entityName + result.toString();
	}

	public String toString(Type[] types, Object[] values) throws HibernateException {
		StringBuilder buffer = new StringBuilder();
		for ( int i=0; i<types.length; i++ ) {
			if ( types[i]!=null ) {
				buffer.append( types[i].toLoggableString( values[i], factory ) ).append( ", " );
			}
		}
		return buffer.toString();
	}

	public String toString(Map<String,TypedValue> namedTypedValues) throws HibernateException {
		Map<String,String> result = new HashMap<String,String>();
		for ( Map.Entry<String, TypedValue> entry : namedTypedValues.entrySet() ) {
			result.put(
					entry.getKey(), entry.getValue().getType().toLoggableString(
					entry.getValue().getValue(),
					factory
			)
			);
		}
		return result.toString();
	}

	// Cannot use Map as an argument because it clashes with the previous method (due to type erasure)
	public void toString(Iterable<Map.Entry<EntityKey,Object>> entitiesByEntityKey) throws HibernateException {
        if ( ! LOG.isDebugEnabled() || ! entitiesByEntityKey.iterator().hasNext() ) return;
        LOG.debug( "Listing entities:" );
		int i=0;
		for (  Map.Entry<EntityKey,Object> entityKeyAndEntity : entitiesByEntityKey ) {
			if (i++>20) {
                LOG.debug("More......");
				break;
			}
            LOG.debug( toString( entityKeyAndEntity.getKey().getEntityName(), entityKeyAndEntity.getValue() ) );
		}
	}

	public EntityPrinter(SessionFactoryImplementor factory) {
		this.factory = factory;
	}
}
