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
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.Type;

import static org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext.EntityReferenceProcessingState;

/**
 * Encapsulates the logic for reading a single entity identifier from a JDBC ResultSet, including support for fetches
 * that are part of the identifier.
 *
 * @author Steve Ebersole
 */
public class EntityIdentifierReaderImpl implements EntityIdentifierReader {
	private static final Logger log = CoreLogging.logger( EntityIdentifierReaderImpl.class );

	private final EntityReference entityReference;
	private final EntityReferenceAliases aliases;
	private final List<EntityReferenceReader> identifierFetchReaders;

	private final boolean isReturn;
	private final Type identifierType;

	/**
	 * Creates a delegate capable of performing the reading of an entity identifier
	 *
	 * @param entityReference The entity reference for which we will be reading the identifier.
	 */
	public EntityIdentifierReaderImpl(
			EntityReference entityReference,
			EntityReferenceAliases aliases,
			List<EntityReferenceReader> identifierFetchReaders) {
		this.entityReference = entityReference;
		this.aliases = aliases;
		this.isReturn = EntityReturn.class.isInstance( entityReference );
		this.identifierType = entityReference.getEntityPersister().getIdentifierType();
		this.identifierFetchReaders = identifierFetchReaders;
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		final EntityReferenceProcessingState processingState = context.getProcessingState( entityReference );

		// if the entity reference we are hydrating is a Return, it is possible that its EntityKey is
		// supplied by the QueryParameter optional entity information
		if ( context.shouldUseOptionalEntityInformation() ) {
			if ( isReturn ) {
				final EntityKey entityKey = ResultSetProcessorHelper.getOptionalObjectKey(
						context.getQueryParameters(),
						context.getSession()
				);

				if ( entityKey != null ) {
					processingState.registerEntityKey( entityKey );
					return;
				}
			}
		}

		// get any previously registered identifier hydrated-state
		Object identifierHydratedForm = processingState.getIdentifierHydratedForm();
		if ( identifierHydratedForm == null ) {
			// if there is none, read it from the result set
			identifierHydratedForm = readIdentifierHydratedState( resultSet, context );

			// broadcast the fact that a hydrated identifier value just became associated with
			// this entity reference
			processingState.registerIdentifierHydratedForm( identifierHydratedForm );
			for ( EntityReferenceReader reader : identifierFetchReaders ) {
				reader.hydrateIdentifier( resultSet, context );
			}
		}
	}

	/**
	 * Read the identifier state for the entity reference for the currently processing row in the ResultSet
	 *
	 * @param resultSet The ResultSet being processed
	 * @param context The processing context
	 *
	 * @return The hydrated state
	 *
	 * @throws java.sql.SQLException Indicates a problem accessing the ResultSet
	 */
	private Object readIdentifierHydratedState(ResultSet resultSet, ResultSetProcessingContext context)
			throws SQLException {
//		if ( EntityReturn.class.isInstance( entityReference ) ) {
//			// if there is a "optional entity key" associated with the context it would pertain to this
//			// entity reference, because it is the root return.
//			final EntityKey suppliedEntityKey = context.getSuppliedOptionalEntityKey();
//			if ( suppliedEntityKey != null ) {
//				return suppliedEntityKey.getIdentifier();
//			}
//		}

		// Otherwise, read it from the ResultSet
		final String[] columnNames;
		if ( EntityFetch.class.isInstance( entityReference )
				&& !FetchStrategyHelper.isJoinFetched( ((EntityFetch) entityReference).getFetchStrategy() ) ) {
			final EntityFetch fetch = (EntityFetch) entityReference;
			final FetchOwner fetchOwner = fetch.getOwner();
			if ( EntityReference.class.isInstance( fetchOwner ) ) {
				throw new NotYetImplementedException();
//					final EntityReference ownerEntityReference = (EntityReference) fetchOwner;
//					final EntityAliases ownerEntityAliases = context.getAliasResolutionContext()
//							.resolveEntityColumnAliases( ownerEntityReference );
//					final int propertyIndex = ownerEntityReference.getEntityPersister()
//							.getEntityMetamodel()
//							.getPropertyIndex( fetch.getOwnerPropertyName() );
//					columnNames = ownerEntityAliases.getSuffixedPropertyAliases()[ propertyIndex ];
			}
			else {
				// todo : better message here...
				throw new WalkingException( "Cannot locate association column names" );
			}
		}
		else {
			columnNames = aliases.getColumnAliases().getSuffixedKeyAliases();
		}

		try {
			return entityReference.getEntityPersister().getIdentifierType().hydrate(
					resultSet,
					columnNames,
					context.getSession(),
					null
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Encountered problem trying to hydrate identifier for entity ["
							+ entityReference.getEntityPersister() + "]",
					e

			);
		}
	}

//	/**
//	 * Hydrate the identifiers of all fetches that are part of this entity reference's identifier (key-many-to-one).
//	 *
//	 * @param resultSet The ResultSet
//	 * @param context The processing context
//	 * @param hydratedIdentifierState The hydrated identifier state of the entity reference.  We can extract the
//	 * fetch identifier's hydrated state from there if available, without having to read the Result (which can
//	 * be a performance problem on some drivers).
//	 */
//	private void hydrateIdentifierFetchIdentifiers(
//			ResultSet resultSet,
//			ResultSetProcessingContext context,
//			Object hydratedIdentifierState) throws SQLException {
//		// for all fetches that are part of our identifier...
//		for ( Fetch fetch : entityReference.getIdentifierDescription().getFetches() ) {
//			hydrateIdentifierFetchIdentifier( resultSet, context, fetch, hydratedIdentifierState );
//		}
//	}
//
//	private void hydrateIdentifierFetchIdentifier(
//			ResultSet resultSet,
//			ResultSetProcessingContext context,
//			Fetch fetch,
//			Object hydratedIdentifierState) throws SQLException {
//		if ( CompositeFetch.class.isInstance( fetch ) ) {
//			for ( Fetch subFetch : ( (CompositeFetch) fetch).getFetches() ) {
//				hydrateIdentifierFetchIdentifier( resultSet, context, subFetch, hydratedIdentifierState );
//			}
//		}
//		else if ( ! EntityFetch.class.isInstance( fetch ) ) {
//			throw new NotYetImplementedException( "Cannot hydrate identifier Fetch that is not an EntityFetch" );
//		}
//		else {
//			final EntityFetch entityFetch = (EntityFetch) fetch;
//			final EntityReferenceProcessingState fetchProcessingState = context.getProcessingState( entityFetch );
//
//			// if the identifier for the fetch was already hydrated, nothing to do
//			if ( fetchProcessingState.getIdentifierHydratedForm() != null ) {
//				return;
//			}
//
//			// we can either hydrate the fetch's identifier from the incoming 'hydratedIdentifierState' (by
//			// extracting the relevant portion using HydratedCompoundValueHandler) or we can
//			// read it from the ResultSet
//			if ( hydratedIdentifierState != null ) {
//				final HydratedCompoundValueHandler hydratedStateHandler = entityReference.getIdentifierDescription().getHydratedStateHandler( fetch );
//				if ( hydratedStateHandler != null ) {
//					final Serializable extracted = (Serializable) hydratedStateHandler.extract( hydratedIdentifierState );
//					fetchProcessingState.registerIdentifierHydratedForm( extracted );
//				}
//			}
//			else {
//				// Use a reader to hydrate the fetched entity.
//				//
//				// todo : Ideally these should be kept around
//				new EntityReferenceReader( entityFetch ).hydrateIdentifier( resultSet, context );
//			}
//		}
//	}


	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		// resolve fetched state from the identifier first
//		for ( EntityReferenceReader reader : identifierFetchReaders ) {
//			reader.resolveEntityKey( resultSet, context );
//		}
//		for ( EntityReferenceReader reader : identifierFetchReaders ) {
//			reader.hydrateEntityState( resultSet, context );
//		}

		final EntityReferenceProcessingState processingState = context.getProcessingState( entityReference );

		// see if we already have an EntityKey associated with this EntityReference in the processing state.
		// if we do, this should have come from the optional entity identifier...
		final EntityKey entityKey = processingState.getEntityKey();
		if ( entityKey != null ) {
			log.debugf(
					"On call to EntityIdentifierReaderImpl#resolve [for %s], EntityKey was already known; " +
							"should only happen on root returns with an optional identifier specified"
			);
			return;
		}

		// Look for the hydrated form
		final Object identifierHydratedForm = processingState.getIdentifierHydratedForm();
		if ( identifierHydratedForm == null ) {
			// we need to register the missing identifier, but that happens later after all readers have had a chance
			// to resolve its EntityKey
			return;
		}

		final Type identifierType = entityReference.getEntityPersister().getIdentifierType();
		final Serializable resolvedId = (Serializable) identifierType.resolve(
				identifierHydratedForm,
				context.getSession(),
				null
		);
		if ( resolvedId != null ) {
			processingState.registerEntityKey(
					context.getSession().generateEntityKey( resolvedId, entityReference.getEntityPersister() )
			);
		}
	}
}
