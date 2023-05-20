/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * Standard implementation of {@link org.hibernate.loader.ast.spi.SingleIdEntityLoader}.
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderStandardImpl<T> extends SingleIdEntityLoaderSupport<T> {
	private EnumMap<LockMode, SingleIdLoadPlan> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<CascadingFetchProfile, SingleIdLoadPlan> selectByInternalCascadeProfile;

	private AtomicInteger nonReusablePlansGenerated = new AtomicInteger();

	public AtomicInteger getNonReusablePlansGenerated() {
		return nonReusablePlansGenerated;
	}

	public SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		// todo (6.0) : consider creating a base AST and "cloning" it
		super( entityDescriptor, sessionFactory );
		// see `org.hibernate.persister.entity.AbstractEntityPersister#createLoaders`
		//		we should pre-load a few - maybe LockMode.NONE and LockMode.READ
		final LockOptions lockOptions = LockOptions.NONE;
		final LoadQueryInfluencers queryInfluencers = new LoadQueryInfluencers( sessionFactory );
		final SingleIdLoadPlan<T> plan = createLoadPlan(
				lockOptions,
				queryInfluencers,
				sessionFactory
		);
		if ( determineIfReusable( lockOptions, queryInfluencers ) ) {
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
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even "internal" fetch profiles.
			nonReusablePlansGenerated.incrementAndGet();
			return createLoadPlan( lockOptions, loadQueryInfluencers, sessionFactory );
		}

		final CascadingFetchProfile enabledCascadingFetchProfile = loadQueryInfluencers.getEnabledCascadingFetchProfile();
		if ( enabledCascadingFetchProfile != null ) {
			if ( LockMode.WRITE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( selectByInternalCascadeProfile == null ) {
					selectByInternalCascadeProfile = new EnumMap<>( CascadingFetchProfile.class );
				}
				else {
					final SingleIdLoadPlan existing = selectByInternalCascadeProfile.get( enabledCascadingFetchProfile );
					if ( existing != null ) {
						//noinspection unchecked
						return existing;
					}
				}

				final SingleIdLoadPlan<T> plan = createLoadPlan(
						lockOptions,
						loadQueryInfluencers,
						sessionFactory
				);
				selectByInternalCascadeProfile.put( enabledCascadingFetchProfile, plan );
				return plan;
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean reusable = determineIfReusable( lockOptions, loadQueryInfluencers );

		if ( reusable ) {
			final SingleIdLoadPlan existing = selectByLockMode.get( lockOptions.getLockMode() );
			if ( existing != null ) {
				//noinspection unchecked
				return existing;
			}

			final SingleIdLoadPlan<T> plan = createLoadPlan(
					lockOptions,
					loadQueryInfluencers,
					sessionFactory
			);
			selectByLockMode.put( lockOptions.getLockMode(), plan );

			return plan;
		}

		nonReusablePlansGenerated.incrementAndGet();
		return createLoadPlan( lockOptions, loadQueryInfluencers, sessionFactory );
	}

	private boolean determineIfReusable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( getLoadable().isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( getLoadable().isAffectedByEnabledFetchProfiles( loadQueryInfluencers ) ) {
			return false;
		}

		//noinspection RedundantIfStatement
		if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
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

		final JdbcParametersList jdbcParameters = jdbcParametersBuilder.build();
		return new SingleIdLoadPlan<>(
				(Loadable) getLoadable(),
				getLoadable().getIdentifierMapping(),
				sqlAst,
				jdbcParameters,
				lockOptions,
				sessionFactory
		);
	}
}
