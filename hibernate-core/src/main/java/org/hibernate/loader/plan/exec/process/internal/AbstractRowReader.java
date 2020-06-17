/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.plan.exec.process.spi.CollectionReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.EntityReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.process.spi.RowReader;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.entity.Loadable;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRowReader implements RowReader {
	private static final Logger log = CoreLogging.logger( AbstractRowReader.class );

	private final List<EntityReferenceInitializer> entityReferenceInitializers;
	private final List<CollectionReferenceInitializer> arrayReferenceInitializers;
	private final List<CollectionReferenceInitializer> collectionReferenceInitializers;

	// cache map for looking up EntityReferenceInitializer by EntityReference to help with resolving
	// bidirectional EntityReference and fetches.
	private final Map<EntityReference,EntityReferenceInitializer> entityInitializerByEntityReference;

	public AbstractRowReader(ReaderCollector readerCollector) {
		if ( CollectionHelper.isNotEmpty( readerCollector.getEntityReferenceInitializers() ) ) {
			entityReferenceInitializers = new ArrayList<EntityReferenceInitializer>(
					readerCollector.getEntityReferenceInitializers()
			);
			entityInitializerByEntityReference =
					new HashMap<EntityReference, EntityReferenceInitializer>( entityReferenceInitializers.size() );
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				entityInitializerByEntityReference.put(
						entityReferenceInitializer.getEntityReference(),
						entityReferenceInitializer
				);
			}
		}
		else {
			entityReferenceInitializers = Collections.<EntityReferenceInitializer>emptyList();
			entityInitializerByEntityReference = Collections.<EntityReference,EntityReferenceInitializer>emptyMap();
		}
		this.arrayReferenceInitializers = CollectionHelper.isNotEmpty( readerCollector.getArrayReferenceInitializers() )
				? new ArrayList<CollectionReferenceInitializer>( readerCollector.getArrayReferenceInitializers() )
				: Collections.<CollectionReferenceInitializer>emptyList();
		this.collectionReferenceInitializers =
				CollectionHelper.isNotEmpty ( readerCollector.getNonArrayCollectionReferenceInitializers() )
				? new ArrayList<CollectionReferenceInitializer>( readerCollector.getNonArrayCollectionReferenceInitializers() )
				: Collections.<CollectionReferenceInitializer>emptyList();
	}

	protected abstract Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context)
			throws SQLException;

	@Override
	public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {

		final boolean hasEntityReferenceInitializers = CollectionHelper.isNotEmpty( entityReferenceInitializers );

		if ( hasEntityReferenceInitializers ) {
			// 	1) allow entity references to resolve identifiers (in 2 steps)
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				entityReferenceInitializer.hydrateIdentifier( resultSet, context );
			}
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				resolveEntityKey(
						resultSet,
						context,
						entityReferenceInitializer
				);
			}

			// 2) allow entity references to resolve their non-identifier hydrated state and entity instance
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				entityReferenceInitializer.hydrateEntityState( resultSet, context );
			}
		}


		// 3) read the logical row

		Object logicalRow = readLogicalRow( resultSet, context );


		// 4) allow arrays, entities and collections after row callbacks
		if ( hasEntityReferenceInitializers ) {
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				entityReferenceInitializer.finishUpRow( resultSet, context );
			}
		}
		if ( collectionReferenceInitializers != null ) {
			for ( CollectionReferenceInitializer collectionReferenceInitializer : collectionReferenceInitializers ) {
				collectionReferenceInitializer.finishUpRow( resultSet, context );
			}
		}
		if ( arrayReferenceInitializers != null ) {
			for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
				arrayReferenceInitializer.finishUpRow( resultSet, context );
			}
		}

		return logicalRow;
	}

	private void resolveEntityKey(
			ResultSet resultSet,
			ResultSetProcessingContextImpl context,
			EntityReferenceInitializer entityReferenceInitializer) throws SQLException {
		final EntityReference entityReference = entityReferenceInitializer.getEntityReference();
		final EntityIdentifierDescription identifierDescription = entityReference.getIdentifierDescription();

		if ( identifierDescription.hasFetches() || identifierDescription.hasBidirectionalEntityReferences() ) {
			resolveEntityKey( resultSet, context, (FetchSource) identifierDescription );
		}
		entityReferenceInitializer.resolveEntityKey( resultSet, context );
	}

	private void resolveEntityKey(
			ResultSet resultSet,
			ResultSetProcessingContextImpl context,
			FetchSource fetchSource) throws SQLException {
		// Resolve any bidirectional entity references first.
		for ( BidirectionalEntityReference bidirectionalEntityReference : fetchSource.getBidirectionalEntityReferences() ) {
			final EntityReferenceInitializer targetEntityReferenceInitializer = entityInitializerByEntityReference.get(
					bidirectionalEntityReference.getTargetEntityReference()
			);
			resolveEntityKey(
					resultSet,
					context,
					targetEntityReferenceInitializer
			);
			targetEntityReferenceInitializer.hydrateEntityState( resultSet, context );
		}
		for ( Fetch fetch : fetchSource.getFetches() ) {
			if ( EntityFetch.class.isInstance( fetch ) ) {
				final EntityFetch entityFetch = (EntityFetch) fetch;
				final EntityReferenceInitializer  entityReferenceInitializer = entityInitializerByEntityReference.get(
						entityFetch
				);
				if ( entityReferenceInitializer != null ) {
					resolveEntityKey(
							resultSet,
							context,
							entityReferenceInitializer
					);
					entityReferenceInitializer.hydrateEntityState( resultSet, context );
				}
			}
			else if ( CompositeFetch.class.isInstance( fetch ) ) {
				resolveEntityKey(
						resultSet,
						context,
						(CompositeFetch) fetch
				);
			}
		}
	}

	@Override
	public void finishUp(ResultSetProcessingContextImpl context, List<AfterLoadAction> afterLoadActionList) {
		final List<HydratedEntityRegistration> hydratedEntityRegistrations = context.getHydratedEntityRegistrationList();

		// for arrays, we should end the collection load before resolving the entities, since the
		// actual array instances are not instantiated during loading
		finishLoadingArrays( context );


		// IMPORTANT: reuse the same event instances for performance!
		final PreLoadEvent preLoadEvent;
		final PostLoadEvent postLoadEvent;
		if ( context.getSession().isEventSource() ) {
			preLoadEvent = new PreLoadEvent( (EventSource) context.getSession() );
			postLoadEvent = new PostLoadEvent( (EventSource) context.getSession() );
		}
		else {
			preLoadEvent = null;
			postLoadEvent = null;
		}

		// now finish loading the entities (2-phase load)
		performTwoPhaseLoad( preLoadEvent, context, hydratedEntityRegistrations );

		// now we can finalize loading collections
		finishLoadingCollections( context );

		// and trigger the afterInitialize() hooks
		afterInitialize( context, hydratedEntityRegistrations );

		// finally, perform post-load operations
		postLoad( postLoadEvent, context, hydratedEntityRegistrations, afterLoadActionList );
	}

	protected void finishLoadingArrays(ResultSetProcessingContextImpl context) {
		for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
			arrayReferenceInitializer.endLoading( context );
		}
	}

	private void performTwoPhaseLoad(
			PreLoadEvent preLoadEvent,
			ResultSetProcessingContextImpl context,
			List<HydratedEntityRegistration> hydratedEntityRegistrations) {
		final int numberOfHydratedObjects = hydratedEntityRegistrations == null
				? 0
				: hydratedEntityRegistrations.size();
		log.tracev( "Total objects hydrated: {0}", numberOfHydratedObjects );

		if ( numberOfHydratedObjects == 0 ) {
			return;
		}

		final SharedSessionContractImplementor session = context.getSession();
		final Iterable<PreLoadEventListener> listeners = session
			.getFactory()
			.getServiceRegistry()
			.getService( EventListenerRegistry.class )
			.getEventListenerGroup( EventType.PRE_LOAD )
			.listeners();

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrations ) {
			TwoPhaseLoad.initializeEntity(
					registration.getInstance(),
					context.isReadOnly(),
					session,
					preLoadEvent,
					listeners
			);
		}
	}

	protected void finishLoadingCollections(ResultSetProcessingContextImpl context) {
		for ( CollectionReferenceInitializer collectionReferenceInitializer : collectionReferenceInitializers ) {
			collectionReferenceInitializer.endLoading( context );
		}
	}

	protected void afterInitialize(ResultSetProcessingContextImpl context,
			List<HydratedEntityRegistration> hydratedEntityRegistrations) {
		if ( hydratedEntityRegistrations == null ) {
			return;
		}

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrations ) {
			TwoPhaseLoad.afterInitialize( registration.getInstance(), context.getSession() );
		}
	}

	protected void postLoad(
			PostLoadEvent postLoadEvent,
			ResultSetProcessingContextImpl context,
			List<HydratedEntityRegistration> hydratedEntityRegistrations,
			List<AfterLoadAction> afterLoadActionList) {
		// Until this entire method is refactored w/ polymorphism, postLoad was
		// split off from initializeEntity.  It *must* occur after
		// endCollectionLoad to ensure the collection is in the
		// persistence context.
		if ( hydratedEntityRegistrations == null ) {
			return;
		}

		final SharedSessionContractImplementor session = context.getSession();

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrations ) {
			TwoPhaseLoad.postLoad( registration.getInstance(), session, postLoadEvent );
			if ( afterLoadActionList != null ) {
				for ( AfterLoadAction afterLoadAction : afterLoadActionList ) {
					afterLoadAction.afterLoad(
							session,
							registration.getInstance(),
							(Loadable) registration.getEntityReference().getEntityPersister()
					);
				}
			}
		}

	}

}
