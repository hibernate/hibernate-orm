/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public final class FetchOptionsHelper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( FetchOptionsHelper.class );

	private FetchOptionsHelper() {
	}

	/**
	 *
	 * @param mappingFetchMode The mapping defined fetch mode
	 * @param type The association type
	 * @param sessionFactory The session factory
	 *
	 * @return the fetch style
	 */
	public static FetchStyle determineFetchStyleByMetadata(
			FetchMode mappingFetchMode,
			AssociationType type,
			SessionFactoryImplementor sessionFactory) {
		if ( !( type instanceof EntityType ) && !( type instanceof CollectionType ) ) {
			return FetchStyle.SELECT;
		}

		if ( mappingFetchMode == FetchMode.JOIN ) {
			return FetchStyle.JOIN;
		}

		if ( type instanceof EntityType ) {
			final EntityPersister persister = (EntityPersister) type.getAssociatedJoinable( sessionFactory );
			if ( persister.isBatchLoadable() ) {
				return FetchStyle.BATCH;
			}
			else if ( mappingFetchMode == FetchMode.SELECT ) {
				return FetchStyle.SELECT;
			}
			else if ( !persister.hasProxy() ) {
				return FetchStyle.JOIN;
			}
		}
		else {
			final CollectionPersister persister = (CollectionPersister) type.getAssociatedJoinable( sessionFactory );
			if ( persister instanceof AbstractCollectionPersister
					&& persister.isSubselectLoadable() ) {
				return FetchStyle.SUBSELECT;
			}
			else if ( persister.getBatchSize() > 0 ) {
				return FetchStyle.BATCH;
			}
		}
		return FetchStyle.SELECT;
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			SessionFactoryImplementor sessionFactory) {
		switch ( style ) {
			case JOIN: {
				return FetchTiming.IMMEDIATE;
			}
			case BATCH:
			case SUBSELECT:
			default: {
				return isSubsequentSelectDelayed( type, sessionFactory )
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			boolean lazy,
			String role,
			SessionFactoryImplementor sessionFactory) {
		switch ( style ) {
			case JOIN: {
				if ( lazy ) {
					log.fetchModeJoinWithLazyWarning( role );
					return FetchTiming.DELAYED;
				}
				return FetchTiming.IMMEDIATE;
			}
			case BATCH:
			case SUBSELECT:
			default: {
				return isSubsequentSelectDelayed( type, sessionFactory )
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
	}

	private static boolean isSubsequentSelectDelayed(AssociationType type, SessionFactoryImplementor sessionFactory) {
		if ( type instanceof AnyType ) {
			// we'd need more context here.  this is only kept as part of the property state on the owning entity
			return false;
		}
		else if ( type instanceof EntityType ) {
			final var entityPersister = (EntityPersister) type.getAssociatedJoinable( sessionFactory );
			return entityPersister.isLazy();
		}
		else {
			final var collectionPersister = (CollectionPersister) type.getAssociatedJoinable( sessionFactory );
			return collectionPersister.isLazy() || collectionPersister.isExtraLazy();
		}
	}

	public static boolean isJoinFetched(FetchOptions fetchOptions) {
		return fetchOptions.getTiming() == FetchTiming.IMMEDIATE
				&& fetchOptions.getStyle() == FetchStyle.JOIN;
	}
}
