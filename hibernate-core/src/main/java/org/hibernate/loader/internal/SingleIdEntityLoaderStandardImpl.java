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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.InternalFetchProfile;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * Standard implementation of SingleIdEntityLoader
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderStandardImpl<T> implements SingleIdEntityLoader<T>, Preparable {
	private final EntityPersister entityDescriptor;

	private EnumMap<LockMode, JdbcSelect> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<InternalFetchProfile,JdbcSelect> selectByInternalCascadeProfile;

	public SingleIdEntityLoaderStandardImpl(EntityPersister entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	public void prepare() {
		// see `org.hibernate.persister.entity.AbstractEntityPersister#createLoaders`
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object key, LockOptions lockOptions, SharedSessionContractImplementor session) {

		// todo (6.0) : see `org.hibernate.loader.internal.StandardSingleIdEntityLoader#load` in "upstream" 6.0 branch
		//		- and integrate as much as possible with the `o.h.loader.plan` stuff leveraging the similarities
		//		between the legacy LoadPlan stuff and DomainResult, Assembler, etc.

		final JdbcSelect jdbcSelect = resolveJdbcSelect( lockOptions, session );

		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	private JdbcSelect resolveJdbcSelect(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( entityDescriptor.isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even "internal" fetch profiles.
			return createJdbcSelect( lockOptions, loadQueryInfluencers, session.getFactory() );
		}

		final InternalFetchProfile enabledInternalFetchProfile = loadQueryInfluencers.getEnabledInternalFetchProfile();
		if ( enabledInternalFetchProfile != null ) {
			if ( LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( selectByInternalCascadeProfile == null ) {
					selectByInternalCascadeProfile = new EnumMap<>( InternalFetchProfile.class );
				}
				return selectByInternalCascadeProfile.computeIfAbsent(
						loadQueryInfluencers.getEnabledInternalFetchProfile(),
						internalFetchProfileType -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getFactory() )
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, loadQueryInfluencers );

		if ( cacheable ) {
			return selectByLockMode.computeIfAbsent(
					lockOptions.getLockMode(),
					lockMode -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getFactory() )
			);
		}

		return createJdbcSelect( lockOptions, loadQueryInfluencers, session.getFactory() );
	}

	private boolean determineIfCacheable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( entityDescriptor.isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( entityDescriptor.isAffectedByEnabledFetchProfiles( loadQueryInfluencers ) ) {
			return false;
		}

		//noinspection RedundantIfStatement
		if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}

	private JdbcSelect createJdbcSelect(
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final MetamodelSelectBuilder selectBuilder = new SelectByEntityIdentifierBuilder(
//				entityDescriptor.getFactory(),
//				entityDescriptor
//		);
//		final SqlAstSelectDescriptor selectDescriptor = selectBuilder
//				.generateSelectStatement( 1, queryInfluencers, lockOptions );
//
//
//		return SqlAstSelectToJdbcSelectConverter.interpret(
//				selectDescriptor,
//				sessionFactory
//		);
	}
}
