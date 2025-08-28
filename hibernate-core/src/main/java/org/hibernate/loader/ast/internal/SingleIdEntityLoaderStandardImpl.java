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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * Standard implementation of {@link org.hibernate.loader.ast.spi.SingleIdEntityLoader}.
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderStandardImpl<T> extends SingleIdEntityLoaderSupport<T> {

	private final EnumMap<LockMode, SingleIdLoadPlan<T>> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<CascadingFetchProfile, SingleIdLoadPlan<T>> selectByInternalCascadeProfile;

	private final BiFunction<LockOptions, LoadQueryInfluencers, SingleIdLoadPlan<T>> loadPlanCreator;

	public SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers loadQueryInfluencers) {
		this( entityDescriptor, loadQueryInfluencers,
				(lockOptions, influencers) ->
						createLoadPlan( entityDescriptor, lockOptions, influencers, influencers.getSessionFactory() ) );
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
		// see org.hibernate.persister.entity.AbstractEntityPersister#createLoaders
		// we should preload a few - maybe LockMode.NONE and LockMode.READ
		final LockOptions noLocking = new LockOptions();
		final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( noLocking, influencers );
		if ( isLoadPlanReusable( noLocking, influencers ) ) {
			selectByLockMode.put( LockMode.NONE, plan );
		}
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
			final SingleIdLoadPlan<T> existing = selectByLockMode.get( lockOptions.getLockMode() );
			if ( existing != null ) {
				return existing;
			}
			else {
				final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( lockOptions, influencers );
				selectByLockMode.put( lockOptions.getLockMode(), plan );
				return plan;
			}
		}
		else {
			return loadPlanCreator.apply( lockOptions, influencers );
		}
	}

	private SingleIdLoadPlan<T> getInternalCascadeLoadPlan(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		final CascadingFetchProfile fetchProfile =
				influencers.getEnabledCascadingFetchProfile();
		if ( selectByInternalCascadeProfile == null ) {
			selectByInternalCascadeProfile = new EnumMap<>( CascadingFetchProfile.class );
		}
		else {
			final SingleIdLoadPlan<T> existing =
					selectByInternalCascadeProfile.get( fetchProfile );
			if ( existing != null ) {
				return existing;
			}
		}
		final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( lockOptions, influencers );
		selectByInternalCascadeProfile.put( fetchProfile, plan );
		return plan;
	}

	private boolean isLoadPlanReusable(LockOptions lockOptions, LoadQueryInfluencers influencers) {
		if ( lockOptions.getLockMode().isPessimistic() && lockOptions.hasNonDefaultOptions() ) {
			return false;
		}
		return !getLoadable().isAffectedByEntityGraph( influencers )
			&& !getLoadable().isAffectedByEnabledFetchProfiles( influencers );
	}

	private static <T> SingleIdLoadPlan<T> createLoadPlan(
			EntityMappingType loadable,
			LockOptions lockOptions,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor factory) {

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
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
		);
		return new SingleIdLoadPlan<>(
				loadable,
				loadable.getIdentifierMapping(),
				sqlAst,
				jdbcParametersBuilder.build(),
				lockOptions,
				factory
		);
	}
}
