/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;


/**
 * @author Steve Ebersole
 */
public final class FetchOptionsHelper {

	private FetchOptionsHelper() {
	}

	public interface AssociationPersisterResolver {
		EntityPersister resolveEntityPersister(EntityType entityType);

		CollectionPersister resolveCollectionPersister(CollectionType collectionType);
	}

	public static AssociationPersisterResolver fromFactory(SessionFactoryImplementor factory) {
		return new AssociationPersisterResolver() {
			@Override
			public EntityPersister resolveEntityPersister(EntityType entityType) {
				return entityType.getAssociatedEntityPersister( factory );
			}

			@Override
			public CollectionPersister resolveCollectionPersister(CollectionType collectionType) {
				return collectionType.getPersister( factory );
			}
		};
	}

	/**
	 *
	 * @param mappingFetchStyle The mapping defined fetch style, or
	 * {@code null} if unspecified
	 * @param type The association type
	 * @param persisterResolver Resolves associated persisters
	 *
	 * @return the fetch style
	 */
	public static FetchStyle determineFetchStyleByMetadata(
			FetchStyle mappingFetchStyle,
			AssociationType type,
			AssociationPersisterResolver persisterResolver) {
		if ( mappingFetchStyle == FetchStyle.JOIN ) {
			return FetchStyle.JOIN;
		}
		else if ( type instanceof EntityType entityType ) {
			if ( mappingFetchStyle == FetchStyle.SUBSELECT ) {
				return FetchStyle.SUBSELECT;
			}
			final var persister = persisterResolver.resolveEntityPersister( entityType );
			if ( persister.isBatchLoadable() ) {
				return FetchStyle.BATCH;
			}
			else if ( mappingFetchStyle == FetchStyle.SELECT ) {
				return FetchStyle.SELECT;
			}
			else if ( !persister.hasProxy() ) {
				return FetchStyle.JOIN;
			}
			else {
				return FetchStyle.SELECT;
			}
		}
		else if ( type instanceof CollectionType collectionType ) {
			final var persister = persisterResolver.resolveCollectionPersister( collectionType );
			if ( persister instanceof AbstractCollectionPersister
					&& persister.isSubselectLoadable() ) {
				return FetchStyle.SUBSELECT;
			}
			else if ( persister.getBatchSize() > 0 ) {
				return FetchStyle.BATCH;
			}
			else {
				return FetchStyle.SELECT;
			}
		}
		else {
			return FetchStyle.SELECT;
		}
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			AssociationPersisterResolver persisterResolver) {
		if ( style == FetchStyle.JOIN ) {
			return FetchTiming.IMMEDIATE;
		}
		else {
			return isSubsequentSelectDelayed( type, persisterResolver )
					? FetchTiming.DELAYED
					: FetchTiming.IMMEDIATE;
		}
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			boolean lazy,
			String role,
			AssociationPersisterResolver persisterResolver) {
		if ( style == FetchStyle.JOIN ) {
			if ( lazy ) {
				CORE_LOGGER.fetchModeJoinWithLazyWarning( role );
				return FetchTiming.DELAYED;
			}
			else {
				return FetchTiming.IMMEDIATE;
			}
		}
		else {
			return isSubsequentSelectDelayed( type, persisterResolver )
					? FetchTiming.DELAYED
					: FetchTiming.IMMEDIATE;
		}
	}

	private static boolean isSubsequentSelectDelayed(
			AssociationType type,
			AssociationPersisterResolver persisterResolver) {
		if ( type instanceof AnyType ) {
			// We'd need more context here.
			// This is only kept as part of the
			// property state on the owning entity
			return false;
		}
		else if ( type instanceof EntityType entityType ) {
			return persisterResolver.resolveEntityPersister( entityType ).isLazy();
		}
		else if ( type instanceof CollectionType collectionType ) {
			final var collectionPersister = persisterResolver.resolveCollectionPersister( collectionType );
			return collectionPersister.isLazy()
				|| collectionPersister.isExtraLazy();
		}
		else {
			return false;
		}
	}
}
