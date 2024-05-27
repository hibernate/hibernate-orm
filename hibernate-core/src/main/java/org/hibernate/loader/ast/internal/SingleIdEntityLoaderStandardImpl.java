/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
		this(
				entityDescriptor,
				loadQueryInfluencers,
				(lockOptions, influencers) -> createLoadPlan( entityDescriptor, lockOptions, influencers, influencers.getSessionFactory() )
		);
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
		final LockOptions lockOptions = LockOptions.NONE;
		final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( LockOptions.NONE, influencers );
		if ( isLoadPlanReusable( lockOptions, influencers ) ) {
			selectByLockMode.put( lockOptions.getLockMode(), plan );
		}
	}

	@Override
	public T load(Object key, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		final SingleIdLoadPlan<T> loadPlan = resolveLoadPlan(
				lockOptions,
				session.getLoadQueryInfluencers(),
				session.getFactory()
		);
		return loadPlan.load( key, readOnly, true, session );
	}

	@Override
	public T load(
			Object key,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final SingleIdLoadPlan<T> loadPlan = resolveLoadPlan(
				lockOptions,
				session.getLoadQueryInfluencers(),
				session.getFactory()
		);
		return loadPlan.load( key, entityInstance, readOnly, false, session );
	}

	@Internal
	public SingleIdLoadPlan<T> resolveLoadPlan(
			LockOptions lockOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		if ( getLoadable().isAffectedByEnabledFilters( loadQueryInfluencers, true ) ) {
			// This case is special because the filters need to be applied in order to
			// properly restrict the SQL/JDBC results.  For this reason it has higher
			// precedence than even "internal" fetch profiles.
			return loadPlanCreator.apply( lockOptions, loadQueryInfluencers );
		}
		else if ( loadQueryInfluencers.hasEnabledCascadingFetchProfile()
				// and if it's a non-exclusive (optimistic) lock
				&& LockMode.PESSIMISTIC_READ.greaterThan( lockOptions.getLockMode() ) ) {
			return getInternalCascadeLoadPlan(
					lockOptions,
					loadQueryInfluencers,
					sessionFactory
			);
		}
		else {
			// otherwise see if the loader for the requested load can be cached,
			// which also means we should look in the cache for an existing one
			return getRegularLoadPlan(
					lockOptions,
					loadQueryInfluencers,
					sessionFactory
			);
		}
	}

	private SingleIdLoadPlan<T> getRegularLoadPlan(
			LockOptions lockOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		if ( isLoadPlanReusable( lockOptions, loadQueryInfluencers )  ) {
			final SingleIdLoadPlan<T> existing = selectByLockMode.get( lockOptions.getLockMode() );
			if ( existing != null ) {
				return existing;
			}
			else {
				final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( lockOptions, loadQueryInfluencers );
				selectByLockMode.put( lockOptions.getLockMode(), plan );
				return plan;
			}
		}
		else {
			return loadPlanCreator.apply( lockOptions, loadQueryInfluencers );
		}
	}

	private SingleIdLoadPlan<T> getInternalCascadeLoadPlan(
			LockOptions lockOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final CascadingFetchProfile fetchProfile =
				loadQueryInfluencers.getEnabledCascadingFetchProfile();

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

		final SingleIdLoadPlan<T> plan = loadPlanCreator.apply( lockOptions, loadQueryInfluencers );
		selectByInternalCascadeProfile.put( fetchProfile, plan );
		return plan;
	}

	private boolean isLoadPlanReusable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		return lockOptions.getTimeOut() == LockOptions.WAIT_FOREVER
			&& !getLoadable().isAffectedByEntityGraph( loadQueryInfluencers )
			&& !getLoadable().isAffectedByEnabledFetchProfiles( loadQueryInfluencers );
	}

	private static <T> SingleIdLoadPlan<T> createLoadPlan(
			EntityMappingType loadable,
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				loadable,
				// null here means to select everything
				null,
				loadable.getIdentifierMapping(),
				null,
				1,
				queryInfluencers,
				lockOptions,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		return new SingleIdLoadPlan<>(
				loadable,
				loadable.getIdentifierMapping(),
				sqlAst,
				jdbcParametersBuilder.build(),
				lockOptions,
				sessionFactory
		);
	}
}
