/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.EnumMap;

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

	public SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		// todo (6.0) : consider creating a base AST and "cloning" it
		super( entityDescriptor, sessionFactory );
		// see org.hibernate.persister.entity.AbstractEntityPersister#createLoaders
		// we should preload a few - maybe LockMode.NONE and LockMode.READ
		final LockOptions lockOptions = LockOptions.NONE;
		final LoadQueryInfluencers queryInfluencers = new LoadQueryInfluencers( sessionFactory );
		final SingleIdLoadPlan<T> plan = createLoadPlan(
				lockOptions,
				queryInfluencers,
				sessionFactory
		);
		if ( isLoadPlanReusable( lockOptions, queryInfluencers ) ) {
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

		if ( getLoadable().isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
			// This case is special because the filters need to be applied in order to
			// properly restrict the SQL/JDBC results.  For this reason it has higher
			// precedence than even "internal" fetch profiles.
			return createLoadPlan( lockOptions, loadQueryInfluencers, sessionFactory );
		}
		else if ( loadQueryInfluencers.hasEnabledCascadingFetchProfile()
				&& LockMode.WRITE.greaterThan( lockOptions.getLockMode() ) ) {
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
				final SingleIdLoadPlan<T> plan = createLoadPlan(
						lockOptions,
						loadQueryInfluencers,
						sessionFactory
				);
				selectByLockMode.put( lockOptions.getLockMode(), plan );
				return plan;
			}
		}
		else {
			return createLoadPlan(lockOptions, loadQueryInfluencers, sessionFactory);
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

		final SingleIdLoadPlan<T> plan = createLoadPlan(
				lockOptions,
				loadQueryInfluencers,
				sessionFactory
		);
		selectByInternalCascadeProfile.put( fetchProfile, plan );
		return plan;
	}

	private boolean isLoadPlanReusable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		return lockOptions.getTimeOut() == LockOptions.WAIT_FOREVER
			&& !getLoadable().isAffectedByEntityGraph( loadQueryInfluencers )
			&& !getLoadable().isAffectedByEnabledFetchProfiles( loadQueryInfluencers );
	}

	private SingleIdLoadPlan<T> createLoadPlan(
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				getLoadable().getIdentifierMapping(),
				null,
				1,
				queryInfluencers,
				lockOptions,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		return new SingleIdLoadPlan<>(
				getLoadable(),
				getLoadable().getIdentifierMapping(),
				sqlAst,
				jdbcParametersBuilder.build(),
				lockOptions,
				sessionFactory
		);
	}
}
