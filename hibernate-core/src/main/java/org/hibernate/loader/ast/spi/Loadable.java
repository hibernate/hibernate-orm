/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.tree.from.RootTableGroupProducer;

/**
 * Common details for things that can be loaded by a {@linkplain Loader loader} - generally
 * {@linkplain org.hibernate.metamodel.mapping.EntityMappingType entities} and
 * {@linkplain org.hibernate.metamodel.mapping.PluralAttributeMapping plural attributes} (collections).
 *
 * @see Loader
 * @see org.hibernate.metamodel.mapping.EntityMappingType
 * @see org.hibernate.metamodel.mapping.PluralAttributeMapping
 *
 * @author Steve Ebersole
 */
public interface Loadable extends ModelPart, RootTableGroupProducer {
	/**
	 * The name for this loadable, for use as the root when generating
	 * {@linkplain org.hibernate.spi.NavigablePath relative paths}
	 */
	String getRootPathName();

	/**
	 * @deprecated Use {@link #isAffectedByInfluencers(LoadQueryInfluencers, boolean)} instead
	 */
	@Deprecated(forRemoval = true)
	default boolean isAffectedByInfluencers(LoadQueryInfluencers influencers) {
		return isAffectedByInfluencers( influencers, false );
	}

	default boolean isAffectedByInfluencers(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return isAffectedByEntityGraph( influencers )
				|| isAffectedByEnabledFetchProfiles( influencers )
				|| isAffectedByEnabledFilters( influencers, onlyApplyForLoadByKeyFilters )
				|| isAffectedByBatchSize( influencers );
	}

	default boolean isNotAffectedByInfluencers(LoadQueryInfluencers influencers) {
		return !isAffectedByEntityGraph( influencers )
			&& !isAffectedByEnabledFetchProfiles( influencers )
			&& !isAffectedByEnabledFilters( influencers )
			&& !isAffectedByBatchSize( influencers )
			&& influencers.getEnabledCascadingFetchProfile() == null;
	}

	private boolean isAffectedByBatchSize(LoadQueryInfluencers influencers) {
		return influencers.getBatchSize() > 0
			&& influencers.getBatchSize() != getBatchSize();
	}

	int getBatchSize();

	/**
	 * Whether any of the "influencers" affect this loadable.
	 * @deprecated Use {@link #isAffectedByEnabledFilters(LoadQueryInfluencers, boolean)} instead
	 */
	@Deprecated(forRemoval = true)
	default boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		return isAffectedByEnabledFilters( influencers, false );
	}

	/**
	 * Whether any of the "influencers" affect this loadable.
	 */
	boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters);

	/**
	 * Whether the {@linkplain LoadQueryInfluencers#getEffectiveEntityGraph() effective entity-graph}
	 * applies to this loadable
	 */
	boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers);

	/**
	 * Whether any of the {@linkplain LoadQueryInfluencers#getEnabledFetchProfileNames()}
	 * apply to this loadable
	 */
	boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers);
}
