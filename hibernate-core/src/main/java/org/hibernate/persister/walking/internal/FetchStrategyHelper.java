/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.internal;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public final class FetchStrategyHelper {
	private FetchStrategyHelper() {
	}

	/**
	 * Determine the fetch-style (if one) explicitly set for this association via fetch profiles.
	 * <p/>
	 * Note that currently fetch profiles only allow specifying join fetching, so this method currently
	 * returns either (a) FetchStyle.JOIN or (b) null
	 *
	 * @param loadQueryInfluencers
	 * @param persister
	 * @param path
	 * @param propertyNumber
	 *
	 * @return
	 */
	public static FetchStyle determineFetchStyleByProfile(
			LoadQueryInfluencers loadQueryInfluencers,
			EntityPersister persister,
			PropertyPath path,
			int propertyNumber) {
		if ( !loadQueryInfluencers.hasEnabledFetchProfiles() ) {
			// perf optimization
			return null;
		}

		// ugh, this stuff has to be made easier...
		final String fullPath = path.getFullPath();
		final String rootPropertyName = ( (OuterJoinLoadable) persister ).getSubclassPropertyName( propertyNumber );
		int pos = fullPath.lastIndexOf( rootPropertyName );
		final String relativePropertyPath = pos >= 0
				? fullPath.substring( pos )
				: rootPropertyName;
		final String fetchRole = persister.getEntityName() + '.' + relativePropertyPath;

		for ( String profileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
			final FetchProfile profile = loadQueryInfluencers.getSessionFactory().getFetchProfile( profileName );
			final Fetch fetch = profile.getFetchByRole( fetchRole );
			if ( fetch != null && Fetch.Style.JOIN == fetch.getStyle() ) {
				return FetchStyle.JOIN;
			}
		}
		return null;
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
		if ( !type.isEntityType() && !type.isCollectionType() ) {
			return FetchStyle.SELECT;
		}

		if ( mappingFetchMode == FetchMode.JOIN ) {
			return FetchStyle.JOIN;
		}

		if ( type.isEntityType() ) {
			EntityPersister persister = (EntityPersister) type.getAssociatedJoinable( sessionFactory );
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
			CollectionPersister persister = (CollectionPersister) type.getAssociatedJoinable( sessionFactory );
			if ( persister instanceof AbstractCollectionPersister
					&& ( (AbstractCollectionPersister) persister ).isSubselectLoadable() ) {
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
			case SUBSELECT: {
				return FetchTiming.DELAYED;
			}
			default: {
				// SELECT case, can be either
				return isSubsequentSelectDelayed( type, sessionFactory )
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
	}

	private static boolean isSubsequentSelectDelayed(AssociationType type, SessionFactoryImplementor sessionFactory) {
		if ( type.isAnyType() ) {
			// we'd need more context here.  this is only kept as part of the property state on the owning entity
			return false;
		}
		else if ( type.isEntityType() ) {
			return ( (EntityPersister) type.getAssociatedJoinable( sessionFactory ) ).hasProxy();
		}
		else {
			final CollectionPersister cp = ( (CollectionPersister) type.getAssociatedJoinable( sessionFactory ) );
			return cp.isLazy() || cp.isExtraLazy();
		}
	}

	public static boolean isJoinFetched(FetchStrategy fetchStrategy) {
		return fetchStrategy.getTiming() == FetchTiming.IMMEDIATE
				&& fetchStrategy.getStyle() == FetchStyle.JOIN;
	}
}
