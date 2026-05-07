/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
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

	/**
	 *
	 * @param mappingFetchMode The mapping defined fetch mode
	 * @param type The association type
	 * @param factory The session factory
	 *
	 * @return the fetch style
	 */
	public static FetchStyle determineFetchStyleByMetadata(
			FetchMode mappingFetchMode,
			AssociationType type,
			SessionFactoryImplementor factory) {
		if ( mappingFetchMode == FetchMode.JOIN ) {
			return FetchStyle.JOIN;
		}
		else if ( type instanceof EntityType entityType ) {
			final var persister = entityType.getAssociatedEntityPersister( factory );
			if ( persister.isBatchLoadable() ) {
				return FetchStyle.BATCH;
			}
			else if ( mappingFetchMode == FetchMode.SELECT ) {
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
			final var persister = collectionType.getPersister( factory );
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
			SessionFactoryImplementor factory) {
		if ( style == FetchStyle.JOIN ) {
			return FetchTiming.IMMEDIATE;
		}
		else {
			return isSubsequentSelectDelayed( type, factory )
					? FetchTiming.DELAYED
					: FetchTiming.IMMEDIATE;
		}
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			boolean lazy,
			String role,
			SessionFactoryImplementor factory) {
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
			return isSubsequentSelectDelayed( type, factory )
					? FetchTiming.DELAYED
					: FetchTiming.IMMEDIATE;
		}
	}

	private static boolean isSubsequentSelectDelayed(AssociationType type, SessionFactoryImplementor factory) {
		if ( type instanceof AnyType ) {
			// We'd need more context here.
			// This is only kept as part of the
			// property state on the owning entity
			return false;
		}
		else if ( type instanceof EntityType entityType ) {
			return entityType.getAssociatedEntityPersister( factory ).isLazy();
		}
		else if ( type instanceof CollectionType collectionType ) {
			final var collectionPersister = collectionType.getPersister( factory );
			return collectionPersister.isLazy()
				|| collectionPersister.isExtraLazy();
		}
		else {
			return false;
		}
	}
}
