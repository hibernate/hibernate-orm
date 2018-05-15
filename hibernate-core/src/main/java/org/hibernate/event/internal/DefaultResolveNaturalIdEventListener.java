/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.ResolveNaturalIdEvent;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.pretty.MessageHelper;

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

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultResolveNaturalIdEventListener.class );

	@Override
	public void onResolveNaturalId(ResolveNaturalIdEvent event) throws HibernateException {
		final Object entityId = resolveNaturalId( event );
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
	protected Object resolveNaturalId(final ResolveNaturalIdEvent event) {
		final EntityTypeDescriptor descriptor = event.getEntityDescriptor();

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.tracev(
					"Attempting to resolve: {0}#{1}",
					MessageHelper.infoString( descriptor ),
					event.getNaturalIdValues()
			);
		}

		Object entityId = resolveFromCache( event );
		if ( entityId != null ) {
			if ( traceEnabled ) {
				LOG.tracev(
						"Resolved object in cache: {0}#{1}",
						MessageHelper.infoString( descriptor ),
						event.getNaturalIdValues()
				);
			}
			return entityId;
		}

		if ( traceEnabled ) {
			LOG.tracev(
					"Object not resolved in any cache: {0}#{1}",
					MessageHelper.infoString( descriptor ),
					event.getNaturalIdValues()
			);
		}

		return loadFromDatasource( event );
	}

	/**
	 * Attempts to resolve the entity id corresponding to the event's natural id values from the session
	 *
	 * @param event The load event
	 *
	 * @return The entity from the cache, or null.
	 */
	protected Object resolveFromCache(final ResolveNaturalIdEvent event) {
		return event.getSession().getPersistenceContext().getNaturalIdHelper().findCachedNaturalIdResolution(
				event.getEntityDescriptor(),
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
	protected Object loadFromDatasource(final ResolveNaturalIdEvent event) {
		final SessionFactoryImplementor factory = event.getSession().getFactory();
		final boolean stats = factory.getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		final Object pk = event.getEntityDescriptor().getNaturalIdLoader().resolveNaturalIdToEntity(
				event.getOrderedNaturalIdValues(),
				event.getLockOptions(),
				event.getSession()
		);

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			factory.getStatistics().naturalIdQueryExecuted(
					event.getEntityDescriptor().getHierarchy().getRootEntityType().getEntityName(),
					milliseconds );
		}

		//PK can be null if the entity doesn't exist
		if (pk != null) {
			event.getSession().getPersistenceContext().getNaturalIdHelper().cacheNaturalIdCrossReferenceFromLoad(
					event.getEntityDescriptor(),
					pk,
					event.getOrderedNaturalIdValues()
			);
		}

		return pk;
	}
}
