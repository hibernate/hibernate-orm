/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext.CachedNaturalIdValueSource;
import org.hibernate.event.spi.ResolveNaturalIdEvent;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.jboss.logging.Logger;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public class DefaultResolveNaturalIdEventListener
		extends AbstractLockUpgradeEventListener
		implements ResolveNaturalIdEventListener {

	public static final Object REMOVED_ENTITY_MARKER = new Object();
	public static final Object INCONSISTENT_RTN_CLASS_MARKER = new Object();

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DefaultResolveNaturalIdEventListener.class.getName()
	);

	@Override
	public void onResolveNaturalId(ResolveNaturalIdEvent event) throws HibernateException {
		final Serializable entityId = resolveNaturalId( event );
		event.setEntityId( entityId );
	}

	/**
	 * Coordinates the efforts to load a given entity. First, an attempt is
	 * made to load the entity from the session-level cache. If not found there,
	 * an attempt is made to locate it in second-level cache. Lastly, an
	 * attempt is made to load it directly from the datasource.
	 * 
	 * @param event The load event
	 *
	 * @return The loaded entity, or null.
	 */
	protected Serializable resolveNaturalId(final ResolveNaturalIdEvent event) {
		final EntityPersister persister = event.getEntityPersister();

		if ( LOG.isTraceEnabled() ) {
			LOG.trace(
					"Attempting to resolve: " +
							MessageHelper.infoString(
									persister, event.getNaturalIdValues(), event.getSession().getFactory()
							)
			);
		}

		Serializable entityId = resolveFromSessionCache( event );
		if ( entityId != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace(
						"Resolved object in session cache: " +
								MessageHelper.infoString(
										persister, event.getNaturalIdValues(), event.getSession().getFactory()
								)
				);
			}
			return entityId;
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.trace(
					"Object not resolved in any cache: " +
							MessageHelper.infoString(
									persister, event.getNaturalIdValues(), event.getSession().getFactory()
							)
			);
		}

		return loadFromDatasource( event );
	}

	/**
	 * Attempts to resolve the entity id corresponding to the event's natural id values from the session
	 * 
	 * @param event The load event
	 *
	 * @return The entity from the session-level cache, or null.
	 */
	protected Serializable resolveFromSessionCache(final ResolveNaturalIdEvent event) {
		return event.getSession().getPersistenceContext().findCachedNaturalIdResolution(
				event.getEntityPersister(),
				event.getOrderedNaturalIdValues()
		);
	}

	/**
	 * Performs the process of loading an entity from the configured
	 * underlying datasource.
	 * 
	 * @param event The load event
	 *
	 * @return The object loaded from the datasource, or null if not found.
	 */
	protected Serializable loadFromDatasource(final ResolveNaturalIdEvent event) {
		final Serializable pk = event.getEntityPersister().loadEntityIdByNaturalId(
				event.getOrderedNaturalIdValues(),
				event.getLockOptions(),
				event.getSession()
		);
		event.getSession().getPersistenceContext().cacheNaturalIdResolution(
				event.getEntityPersister(),
				pk,
				event.getOrderedNaturalIdValues(),
				CachedNaturalIdValueSource.LOAD
		);
		return pk;
	}
}
