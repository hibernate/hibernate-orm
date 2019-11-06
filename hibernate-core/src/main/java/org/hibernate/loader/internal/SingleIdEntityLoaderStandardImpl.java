/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.EnumMap;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.InternalFetchProfile;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard implementation of SingleIdEntityLoader
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderStandardImpl<T> extends SingleIdEntityLoaderSupport<T> implements Preparable {
	private EnumMap<LockMode, SingleIdLoadPlan> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<InternalFetchProfile, SingleIdLoadPlan> selectByInternalCascadeProfile;

	public SingleIdEntityLoaderStandardImpl(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
	}

	@Override
	public void prepare() {
		// see `org.hibernate.persister.entity.AbstractEntityPersister#createLoaders`

	}

	@Override
	public T load(Object key, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final SingleIdLoadPlan<T> loadPlan = resolveLoadPlan( lockOptions, session );

		return loadPlan.load( key, lockOptions, session );
	}

	private SingleIdLoadPlan<T> resolveLoadPlan(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( getLoadable().isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even "internal" fetch profiles.
			return createLoadPlan( lockOptions, loadQueryInfluencers, session.getFactory() );
		}

		final InternalFetchProfile enabledInternalFetchProfile = loadQueryInfluencers.getEnabledInternalFetchProfile();
		if ( enabledInternalFetchProfile != null ) {
			if ( LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( selectByInternalCascadeProfile == null ) {
					selectByInternalCascadeProfile = new EnumMap<>( InternalFetchProfile.class );
				}
				else {
					final SingleIdLoadPlan existing = selectByInternalCascadeProfile.get( enabledInternalFetchProfile );
					if ( existing != null ) {
						//noinspection unchecked
						return existing;
					}
				}

				final SingleIdLoadPlan<T> plan = createLoadPlan(
						lockOptions,
						loadQueryInfluencers,
						session.getFactory()
				);
				selectByInternalCascadeProfile.put( enabledInternalFetchProfile, plan );

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
					session.getFactory()
			);
			selectByLockMode.put( lockOptions.getLockMode(), plan );

			return plan;
		}

		return createLoadPlan( lockOptions, loadQueryInfluencers, session.getFactory() );
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
		final MetamodelSelectBuilderProcess.SqlAstDescriptor sqlAstDescriptor = MetamodelSelectBuilderProcess.createSelect(
				sessionFactory,
				getLoadable(),
				null,
				getLoadable().getIdentifierMapping(),
				null,
				1,
				queryInfluencers,
				lockOptions
		);

		return new SingleIdLoadPlan<>(
				getLoadable().getIdentifierMapping(),
				sqlAstDescriptor
		);
	}
}
