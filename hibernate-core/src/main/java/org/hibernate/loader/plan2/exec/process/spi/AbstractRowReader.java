/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan2.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.plan2.exec.process.internal.HydratedEntityRegistration;
import org.hibernate.loader.plan2.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan2.spi.BidirectionalEntityFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.entity.Loadable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRowReader implements RowReader {
	private static final Logger log = CoreLogging.logger( AbstractRowReader.class );

	protected abstract List<EntityReferenceInitializer> getEntityReferenceInitializers();
	protected abstract List<CollectionReferenceInitializer> getArrayReferenceInitializers();
	protected abstract List<CollectionReferenceInitializer> getCollectionReferenceInitializers();

	protected abstract Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context)
			throws SQLException;

	@Override
	public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
		final List<EntityReferenceInitializer> entityReferenceInitializers = getEntityReferenceInitializers();
		final List<CollectionReferenceInitializer> arrayReferenceInitializers = getArrayReferenceInitializers();
		final List<CollectionReferenceInitializer> collectionReferenceInitializers = getCollectionReferenceInitializers();

		final boolean hasEntityReferenceInitializers = CollectionHelper.isNotEmpty( entityReferenceInitializers );

		if ( hasEntityReferenceInitializers ) {
			// 	1) allow entity references to resolve identifiers (in 2 steps)
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				entityReferenceInitializer.hydrateIdentifier( resultSet, context );
			}
			final Map<EntityReference,EntityReferenceInitializer> initializerByEntityReference =
					new HashMap<EntityReference, EntityReferenceInitializer>( entityReferenceInitializers.size() );
			for ( EntityReferenceInitializer entityReferenceInitializerFromMap : entityReferenceInitializers ) {
				initializerByEntityReference.put( entityReferenceInitializerFromMap.getEntityReference(), entityReferenceInitializerFromMap );
			}
			for ( EntityReferenceInitializer entityReferenceInitializer : entityReferenceInitializers ) {
				resolveEntityKey(
						resultSet,
						context,
						entityReferenceInitializer,
						initializerByEntityReference
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
			EntityReferenceInitializer entityReferenceInitializer,
			Map<EntityReference,EntityReferenceInitializer> initializerByEntityReference) throws SQLException {
		final EntityReference entityReference = entityReferenceInitializer.getEntityReference();
		final EntityIdentifierDescription identifierDescription = entityReference.getIdentifierDescription();

		if ( identifierDescription.hasFetches() ) {
			resolveEntityKey( resultSet, context, (FetchSource) identifierDescription, initializerByEntityReference );
		}
		entityReferenceInitializer.resolveEntityKey( resultSet, context );
	}

	private void resolveEntityKey(
			ResultSet resultSet,
			ResultSetProcessingContextImpl context,
			FetchSource fetchSource,
			Map<EntityReference,EntityReferenceInitializer> initializerByEntityReference) throws SQLException {
		for ( Fetch fetch : fetchSource.getFetches() ) {
			if ( EntityFetch.class.isInstance( fetch ) ) {
				final EntityFetch entityFetch = (EntityFetch) fetch;
				final EntityReferenceInitializer targetEntityReferenceInitializer;
				if ( BidirectionalEntityFetch.class.isInstance( fetch ) ) {
					final BidirectionalEntityFetch bidirectionalEntityFetch = (BidirectionalEntityFetch) fetch;
					targetEntityReferenceInitializer = initializerByEntityReference.get(
							bidirectionalEntityFetch.getTargetEntityReference()
					);
				}
				else {
					targetEntityReferenceInitializer = initializerByEntityReference.get( entityFetch );
				}
				if ( targetEntityReferenceInitializer != null ) {
					resolveEntityKey(
							resultSet,
							context,
							targetEntityReferenceInitializer,
							initializerByEntityReference
					);
					targetEntityReferenceInitializer.hydrateEntityState( resultSet, context );
				}
			}
			else if ( CompositeFetch.class.isInstance( fetch ) ) {
				resolveEntityKey(
						resultSet,
						context,
						(CompositeFetch) fetch,
						initializerByEntityReference );
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

		// finally, perform post-load operations
		postLoad( postLoadEvent, context, hydratedEntityRegistrations, afterLoadActionList );
	}

	private void finishLoadingArrays(ResultSetProcessingContextImpl context) {
		final List<CollectionReferenceInitializer> arrayReferenceInitializers = getArrayReferenceInitializers();
		if ( arrayReferenceInitializers != null ) {
			for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
				arrayReferenceInitializer.endLoading( context );
			}
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

		if ( hydratedEntityRegistrations == null ) {
			return;
		}

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrations ) {
			TwoPhaseLoad.initializeEntity(
					registration.getInstance(),
					context.isReadOnly(),
					context.getSession(),
					preLoadEvent
			);
		}
	}

	private void finishLoadingCollections(ResultSetProcessingContextImpl context) {
		final List<CollectionReferenceInitializer> collectionReferenceInitializers = getCollectionReferenceInitializers();
		if ( collectionReferenceInitializers != null ) {
			for ( CollectionReferenceInitializer arrayReferenceInitializer : collectionReferenceInitializers ) {
				arrayReferenceInitializer.endLoading( context );
			}
		}
	}

	private void postLoad(
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

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrations ) {
			TwoPhaseLoad.postLoad( registration.getInstance(), context.getSession(), postLoadEvent );
			if ( afterLoadActionList != null ) {
				for ( AfterLoadAction afterLoadAction : afterLoadActionList ) {
					afterLoadAction.afterLoad(
							context.getSession(),
							registration.getInstance(),
							(Loadable) registration.getEntityReference().getEntityPersister()
					);
				}
			}
		}

	}

}
