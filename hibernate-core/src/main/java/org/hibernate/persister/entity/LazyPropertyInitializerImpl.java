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
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class LazyPropertyInitializerImpl implements LazyPropertyInitializer {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AbstractEntityPersister.class.getName()
	);
	private final AbstractEntityPersister entityPersister;

	LazyPropertyInitializerImpl(AbstractEntityPersister entityPersister) {
		this.entityPersister = entityPersister;
	}

	@Override
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session)
			throws HibernateException {

		final Serializable id = session.getContextEntityIdentifier( entity );

		final EntityEntry entry = session.getPersistenceContext().getEntry( entity );
		if ( entry == null ) {
			throw new HibernateException( "entity is not associated with the session: " + id );
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Initializing lazy properties of: {0}, field access: {1}", MessageHelper.infoString(
					entityPersister,
					id,
					entityPersister.getFactory()
			), fieldName
			);
		}

		if ( entityPersister.hasCache() ) {
			CacheKey cacheKey = session.generateCacheKey(
					id,
					entityPersister.getIdentifierType(),
					entityPersister.getEntityName()
			);
			Object ce = entityPersister.getCacheAccessStrategy().get( cacheKey, session.getTimestamp() );
			if ( ce != null ) {
				CacheEntry cacheEntry = (CacheEntry) entityPersister.getCacheEntryStructure()
						.destructure( ce, entityPersister.getFactory() );
				if ( !cacheEntry.areLazyPropertiesUnfetched() ) {
					//note early exit here:
					return initializeLazyPropertiesFromCache( fieldName, entity, session, entry, cacheEntry );
				}
			}
		}

		return initializeLazyPropertiesFromDatastore( fieldName, entity, session, id, entry );

	}

	private Object initializeLazyPropertiesFromDatastore(
			final String fieldName,
			final Object entity,
			final SessionImplementor session,
			final Serializable id,
			final EntityEntry entry) {

		if ( !entityPersister.hasLazyProperties() ) {
			throw new AssertionFailure( "no lazy properties" );
		}

		LOG.trace( "Initializing lazy properties from datastore" );

		try {

			Object result = null;
			PreparedStatement ps = null;
			try {
				final String lazySelect = entityPersister.getSQLLazySelectString();
				ResultSet rs = null;
				try {
					if ( lazySelect != null ) {
						// null sql means that the only lazy properties
						// are shared PK one-to-one associations which are
						// handled differently in the Type#nullSafeGet code...
						ps = session.getTransactionCoordinator()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( lazySelect );
						entityPersister.getIdentifierType().nullSafeSet( ps, id, 1, session );
						rs = ps.executeQuery();
						rs.next();
					}
					final Object[] snapshot = entry.getLoadedState();
					for ( int j = 0; j < entityPersister.getLazyPropertyNames().length;
						  j++ ) {
						Object propValue = entityPersister.getLazyPropertyTypes()[j].nullSafeGet(
								rs,
								entityPersister.getLazyPropertyColumnAliases()[j],
								session,
								entity
						);
						if ( initializeLazyProperty( fieldName, entity, session, snapshot, j, propValue ) ) {
							result = propValue;
						}
					}
				}
				finally {
					if ( rs != null ) {
						rs.close();
					}
				}
			}
			finally {
				if ( ps != null ) {
					ps.close();
				}
			}

			LOG.trace( "Done initializing lazy properties" );

			return result;

		}
		catch ( SQLException sqle ) {
			throw entityPersister.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not initialize lazy properties: " +
							MessageHelper.infoString( entityPersister, id, entityPersister.getFactory() ),
					entityPersister.getSQLLazySelectString()
			);
		}
	}

	private Object initializeLazyPropertiesFromCache(
			final String fieldName,
			final Object entity,
			final SessionImplementor session,
			final EntityEntry entry,
			final CacheEntry cacheEntry
	) {

		LOG.trace( "Initializing lazy properties from second-level cache" );

		Object result = null;
		Serializable[] disassembledValues = cacheEntry.getDisassembledState();
		final Object[] snapshot = entry.getLoadedState();
		for ( int j = 0; j < entityPersister.getLazyPropertyNames().length; j++ ) {
			final Object propValue = entityPersister.getLazyPropertyTypes()[j].assemble(
					disassembledValues[entityPersister.getLazyPropertyNumbers()[j]],
					session,
					entity
			);
			if ( initializeLazyProperty( fieldName, entity, session, snapshot, j, propValue ) ) {
				result = propValue;
			}
		}

		LOG.trace( "Done initializing lazy properties" );

		return result;
	}

	private boolean initializeLazyProperty(
			final String fieldName,
			final Object entity,
			final SessionImplementor session,
			final Object[] snapshot,
			final int j,
			final Object propValue) {
		entityPersister.setPropertyValue( entity, entityPersister.getLazyPropertyNumbers()[j], propValue );
		if ( snapshot != null ) {
			// object have been loaded with setReadOnly(true); HHH-2236
			snapshot[entityPersister.getLazyPropertyNumbers()[j]] = entityPersister.getLazyPropertyTypes()[j].deepCopy(
					propValue,
					entityPersister.getFactory()
			);
		}
		return fieldName.equals( entityPersister.getLazyPropertyNames()[j] );
	}
}
