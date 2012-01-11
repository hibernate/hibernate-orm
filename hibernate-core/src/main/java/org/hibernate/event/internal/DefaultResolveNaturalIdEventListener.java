/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.ResolveNaturalIdEvent;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.jboss.logging.Logger;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 * 
 * @author Eric Dalquist
 */
public class DefaultResolveNaturalIdEventListener extends AbstractLockUpgradeEventListener implements
		ResolveNaturalIdEventListener {

	public static final Object REMOVED_ENTITY_MARKER = new Object();
	public static final Object INCONSISTENT_RTN_CLASS_MARKER = new Object();
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			DefaultResolveNaturalIdEventListener.class.getName() );

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.event.spi.ResolveNaturalIdEventListener#onResolveNaturalId(org.hibernate.event.spi.
	 * ResolveNaturalIdEvent)
	 */
	@Override
	public void onResolveNaturalId(ResolveNaturalIdEvent event) throws HibernateException {
		final SessionImplementor source = event.getSession();

		EntityPersister persister = source.getFactory().getEntityPersister( event.getEntityClassName() );
		if ( persister == null ) {
			throw new HibernateException( "Unable to locate persister: " + event.getEntityClassName() );
		}

		// Verify that the entity has a natural id and that the properties match up with the event.
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		final int[] naturalIdentifierProperties = entityMetamodel.getNaturalIdentifierProperties();
		if ( naturalIdentifierProperties == null || naturalIdentifierProperties.length == 0 ) {
			throw new HibernateException( event.getEntityClassName() + " does not have a natural id" );
		}

		final Map<String, Object> naturalIdParams = event.getNaturalId();
		if ( naturalIdentifierProperties.length != naturalIdParams.size() ) {
			throw new HibernateException( event.getEntityClassName() + " has " + naturalIdentifierProperties.length
					+ " properties in its natural id but " + naturalIdParams.size() + " properties were specified: "
					+ naturalIdParams );
		}

		final StandardProperty[] properties = entityMetamodel.getProperties();
		for ( int idPropIdx = 0; idPropIdx < naturalIdentifierProperties.length; idPropIdx++ ) {
			final StandardProperty property = properties[naturalIdentifierProperties[idPropIdx]];
			final String name = property.getName();
			if ( !naturalIdParams.containsKey( name ) ) {
				throw new HibernateException( event.getEntityClassName() + " natural id property " + name
						+ " is missing from the map of natural id parameters: " + naturalIdParams );
			}
		}

		final Serializable entityId = doResolveNaturalId( event, persister );
		event.setEntityId( entityId );
	}

	/**
	 * Coordinates the efforts to load a given entity. First, an attempt is
	 * made to load the entity from the session-level cache. If not found there,
	 * an attempt is made to locate it in second-level cache. Lastly, an
	 * attempt is made to load it directly from the datasource.
	 * 
	 * @param event
	 *            The load event
	 * @param persister
	 *            The persister for the entity being requested for load
	 * @param keyToLoad
	 *            The EntityKey representing the entity to be loaded.
	 * @param options
	 *            The load options.
	 * @return The loaded entity, or null.
	 */
	protected Serializable doResolveNaturalId(final ResolveNaturalIdEvent event, final EntityPersister persister) {

		if ( LOG.isTraceEnabled() )
			LOG.trace( "Attempting to resolve: "
					+ MessageHelper.infoString( persister, event.getNaturalId(), event.getSession().getFactory() ) );

		Serializable entityId = loadFromSessionCache( event, persister );
		if ( entityId == REMOVED_ENTITY_MARKER ) {
			LOG.debugf( "Load request found matching entity in context, but it is scheduled for removal; returning null" );
			return null;
		}
		if ( entityId == INCONSISTENT_RTN_CLASS_MARKER ) {
			LOG.debugf( "Load request found matching entity in context, but the matched entity was of an inconsistent return type; returning null" );
			return null;
		}
		if ( entityId != null ) {
			if ( LOG.isTraceEnabled() )
				LOG.trace( "Resolved object in session cache: "
						+ MessageHelper.infoString( persister, event.getNaturalId(), event.getSession().getFactory() ) );
			return entityId;
		}

		entityId = loadFromSecondLevelCache( event, persister );
		if ( entityId != null ) {
			if ( LOG.isTraceEnabled() )
				LOG.trace( "Resolved object in second-level cache: "
						+ MessageHelper.infoString( persister, event.getNaturalId(), event.getSession().getFactory() ) );
			return entityId;
		}

		if ( LOG.isTraceEnabled() )
			LOG.trace( "Object not resolved in any cache: "
					+ MessageHelper.infoString( persister, event.getNaturalId(), event.getSession().getFactory() ) );

		return loadFromDatasource( event, persister );
	}

	/**
	 * Attempts to locate the entity in the session-level cache.
	 * <p/>
	 * If allowed to return nulls, then if the entity happens to be found in the session cache, we check the entity type
	 * for proper handling of entity hierarchies.
	 * <p/>
	 * If checkDeleted was set to true, then if the entity is found in the session-level cache, it's current status
	 * within the session cache is checked to see if it has previously been scheduled for deletion.
	 * 
	 * @param event
	 *            The load event
	 * @param keyToLoad
	 *            The EntityKey representing the entity to be loaded.
	 * @param options
	 *            The load options.
	 * @return The entity from the session-level cache, or null.
	 * @throws HibernateException
	 *             Generally indicates problems applying a lock-mode.
	 */
	protected Serializable loadFromSessionCache(final ResolveNaturalIdEvent event, final EntityPersister persister)
			throws HibernateException {
		// SessionImplementor session = event.getSession();
		// Object old = session.getEntityUsingInterceptor( keyToLoad );
		//
		// if ( old != null ) {
		// // this object was already loaded
		// EntityEntry oldEntry = session.getPersistenceContext().getEntry( old );
		// if ( options.isCheckDeleted() ) {
		// Status status = oldEntry.getStatus();
		// if ( status == Status.DELETED || status == Status.GONE ) {
		// return REMOVED_ENTITY_MARKER;
		// }
		// }
		// if ( options.isAllowNulls() ) {
		// final EntityPersister persister = event.getSession().getFactory().getEntityPersister(
		// keyToLoad.getEntityName() );
		// if ( ! persister.isInstance( old ) ) {
		// return INCONSISTENT_RTN_CLASS_MARKER;
		// }
		// }
		// upgradeLock( old, oldEntry, event.getLockOptions(), event.getSession() );
		// }

		return null;
	}

	/**
	 * Attempts to load the entity from the second-level cache.
	 * 
	 * @param event
	 *            The load event
	 * @param persister
	 *            The persister for the entity being requested for load
	 * @param options
	 *            The load options.
	 * @return The entity from the second-level cache, or null.
	 */
	protected Serializable loadFromSecondLevelCache(final ResolveNaturalIdEvent event, final EntityPersister persister) {

		// final SessionImplementor source = event.getSession();
		//
		// final boolean useCache = persister.hasCache()
		// && source.getCacheMode().isGetEnabled();
		//
		// if ( useCache ) {
		//
		// final SessionFactoryImplementor factory = source.getFactory();
		//
		// final CacheKey ck = source.generateCacheKey(
		// event.getNaturalId(),
		// persister.getIdentifierType(),
		// persister.getRootEntityName()
		// );
		// Object ce = persister.getCacheAccessStrategy().get( ck, source.getTimestamp() );
		// if ( factory.getStatistics().isStatisticsEnabled() ) {
		// if ( ce == null ) {
		// factory.getStatisticsImplementor().secondLevelCacheMiss(
		// persister.getCacheAccessStrategy().getRegion().getName()
		// );
		// }
		// else {
		// factory.getStatisticsImplementor().secondLevelCacheHit(
		// persister.getCacheAccessStrategy().getRegion().getName()
		// );
		// }
		// }
		//
		// if ( ce != null ) {
		// CacheEntry entry = (CacheEntry) persister.getCacheEntryStructure().destructure( ce, factory );
		//
		// // Entity was found in second-level cache...
		// return assembleCacheEntry(
		// entry,
		// event.getEntityId(),
		// persister,
		// event
		// );
		// }
		// }

		return null;
	}

	/**
	 * Performs the process of loading an entity from the configured
	 * underlying datasource.
	 * 
	 * @param event
	 *            The load event
	 * @param persister
	 *            The persister for the entity being requested for load
	 * @param keyToLoad
	 *            The EntityKey representing the entity to be loaded.
	 * @param options
	 *            The load options.
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Serializable loadFromDatasource(final ResolveNaturalIdEvent event, final EntityPersister persister) {
		final SessionImplementor source = event.getSession();

		return persister.loadEntityIdByNaturalId( event.getNaturalId(), event.getLockOptions(), event.getSession() );
	}
}
