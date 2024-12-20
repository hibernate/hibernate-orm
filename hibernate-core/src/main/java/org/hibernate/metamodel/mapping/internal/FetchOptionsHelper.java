/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

import static org.hibernate.engine.FetchStyle.JOIN;

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
			return JOIN;
		}

		if ( type instanceof EntityType ) {
			EntityPersister persister = (EntityPersister) type.getAssociatedJoinable( sessionFactory );
			if ( persister.isBatchLoadable() ) {
				return FetchStyle.BATCH;
			}
			else if ( mappingFetchMode == FetchMode.SELECT ) {
				return FetchStyle.SELECT;
			}
			else if ( !persister.hasProxy() ) {
				return JOIN;
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
		return determineFetchTiming( style, type, false, sessionFactory );
	}

	public static FetchTiming determineFetchTiming(
			FetchStyle style,
			AssociationType type,
			boolean lazy,
			SessionFactoryImplementor sessionFactory) {
		if ( style == JOIN ) {
			if ( lazy ) {
				return FetchTiming.DELAYED;
//				throw new AssertionFailure("JOIN FetchStyle with LAZY fetching");
			}
			return FetchTiming.IMMEDIATE;
		}
		else {
			return isSubsequentSelectDelayed( type, sessionFactory )
					? FetchTiming.DELAYED
					: FetchTiming.IMMEDIATE;
		}
	}

	private static boolean isSubsequentSelectDelayed(AssociationType type, SessionFactoryImplementor sessionFactory) {
		if ( type instanceof AnyType ) {
			// we'd need more context here.  this is only kept as part of the property state on the owning entity
			return false;
		}
		else if ( type instanceof EntityType ) {
			final EntityPersister entityPersister = (EntityPersister) type.getAssociatedJoinable( sessionFactory );
			return entityPersister.getEntityMetamodel().isLazy();
		}
		else {
			final CollectionPersister cp = ( (CollectionPersister) type.getAssociatedJoinable( sessionFactory ) );
			return cp.isLazy() || cp.isExtraLazy();
		}
	}

	public static boolean isJoinFetched(FetchOptions fetchOptions) {
		return fetchOptions.getTiming() == FetchTiming.IMMEDIATE
			&& fetchOptions.getStyle() == JOIN;
	}
}
