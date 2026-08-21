/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.EnumMap;
import java.util.function.BiFunction;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * Standard implementation of {@link org.hibernate.loader.ast.spi.SingleIdEntityLoader}.
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderStandardImpl<T> extends SingleIdEntityLoaderSupport<T> {

	private final EnumMap<LockMode, SingleIdLoadPlan<T>> selectByLockMode =
			new EnumMap<>( LockMode.class );
	private final EnumMap<CascadingFetchProfile, EnumMap<LockMode,SingleIdLoadPlan<T>>> selectByInternalCascadeProfile =
			new EnumMap<>( CascadingFetchProfile.class );

	private final BiFunction<LockOptions, LoadQueryInfluencers, SingleIdLoadPlan<T>> loadPlanCreator;

	public SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers loadQueryInfluencers) {
		this( entityDescriptor, loadQueryInfluencers,
				(lockOptions, influencers) -> createLoadPlan( entityDescriptor, lockOptions, influencers) );
	}

	/**
	 * For Hibernate Reactive.
	 * <p>
	 * Hibernate Reactive needs to be able to override the LoadPlan.
	 * </p>
	 */
	protected SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers influencers,
			BiFunction<LockOptions, LoadQueryInfluencers, SingleIdLoadPlan<T>> loadPlanCreator) {
		// todo (6.0) : consider creating a base AST and "cloning" it
		super( entityDescriptor, influencers.getSessionFactory() );
		this.loadPlanCreator = loadPlanCreator;
		// Preload some load plans (for now only do it for LockMode.NONE)
		final var singleIdLoadPlan = loadPlanCreator.apply( LockOptions.NONE, influencers );
//		if ( isLoadPlanReusable( LockOptions.NONE, influencers ) ) {
		selectByLockMode.put( LockMode.NONE, singleIdLoadPlan );
//		}
	}

	@Override
	public T load(Object key, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		return resolveLoadPlan( lockOptions, session.getLoadQueryInfluencers() )
				.load( key, readOnly, true, session );
	}

	@Override
	public T load(
			Object key,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		return resolveLoadPlan( lockOptions, session.getLoadQueryInfluencers() )
				.load( key, entityInstance, readOnly, false, session );
	}

	@Internal // public for tests, also called by Hibernate Reactive
	public SingleIdLoadPlan<T> resolveLoadPlan(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		if ( getLoadable().isAffectedByEnabledFilters( influencers, true ) ) {
			// This case is special because the filters need to be applied in order to
			// properly restrict the SQL/JDBC results.  For this reason it has higher
			// precedence than even "internal" fetch profiles.
			return loadPlanCreator.apply( lockOptions, influencers );
		}
		else if ( influencers.hasEnabledCascadingFetchProfile()
				// and if it's a non-exclusive (optimistic) lock
				&& LockMode.PESSIMISTIC_READ.greaterThan( lockOptions.getLockMode() ) ) {
			return getInternalCascadeLoadPlan( lockOptions, influencers );
		}
		else {
			// otherwise see if the loader for the requested load can be cached,
			// which also means we should look in the cache for an existing one
			return getRegularLoadPlan( lockOptions, influencers );
		}
	}

	private SingleIdLoadPlan<T> getRegularLoadPlan(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		if ( isLoadPlanReusable( lockOptions, influencers )  ) {
			final var existing = selectByLockMode.get( lockOptions.getLockMode() );
			if ( existing != null ) {
				return existing;
			}
			else {
				final var singleIdLoadPlan = loadPlanCreator.apply( lockOptions, influencers );
				selectByLockMode.put( lockOptions.getLockMode(), singleIdLoadPlan );
				return singleIdLoadPlan;
			}
		}
		else {
			return loadPlanCreator.apply( lockOptions, influencers );
		}
	}

	private SingleIdLoadPlan<T> getInternalCascadeLoadPlan(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		// TODO: It might be more efficient to just instantiate a LoadPlanKey
		//       object here than it is to maintain an EnumMap of EnumMaps
		final var lockMode = lockOptions.getLockMode();
		EnumMap<LockMode,SingleIdLoadPlan<T>> map;
		if ( isLoadPlanReusable( lockOptions, influencers ) ) {
			final var fetchProfile = influencers.getEnabledCascadingFetchProfile();
			final var existingMap = selectByInternalCascadeProfile.get( fetchProfile );
			if ( existingMap == null ) {
				map = new EnumMap<>( LockMode.class );
				selectByInternalCascadeProfile.put( fetchProfile, map );
			}
			else {
				final var existing = existingMap.get( lockMode );
				if ( existing != null ) {
					return existing;
				}
				else {
					map = existingMap;
				}
			}
		}
		else {
			map = null;
		}

		final var plan = loadPlanCreator.apply( lockOptions, influencers );
		if ( map != null ) {
			map.put( lockMode, plan );
		}
		return plan;
	}

	/**
	 * We key the caches only by {@link LockMode} and {@link CascadingFetchProfile}.
	 * If there is a pessimistic lock with non-default options like timeout, a custom
	 * fetch profile, or an entity graph, we don't cache and reuse the plan.
	 */
	private boolean isLoadPlanReusable(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		if ( lockOptions.getLockMode().isPessimistic() && lockOptions.hasNonDefaultOptions() ) {
			return false;
		}
		else {
			final var loadable = getLoadable();
			return !loadable.isAffectedByEntityGraph( influencers )
				&& !loadable.isAffectedByEnabledFetchProfiles( influencers );
		}
	}

	private static <T> SingleIdLoadPlan<T> createLoadPlan(
			EntityMappingType loadable,
			LockOptions lockOptions,
			LoadQueryInfluencers influencers) {
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final var factory = influencers.getSessionFactory();
		return new SingleIdLoadPlan<>(
				loadable,
				loadable.getIdentifierMapping(),
				LoaderSelectBuilder.createSelect(
						loadable,
						// null here means to select everything
						null,
						loadable.getIdentifierMapping(),
						null,
						1,
						influencers,
						lockOptions,
						jdbcParametersBuilder::add,
						factory
				),
				jdbcParametersBuilder.build(),
				lockOptions,
				factory
		);
	}
}
