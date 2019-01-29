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
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.Writeable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByCollectionKeyBuilder;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.LoadParameterBindingContext;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class CollectionLoaderImpl implements CollectionLoader {
	private final PluralPersistentAttribute pluralAttribute;
	private final SelectByCollectionKeyBuilder selectionBuilder;

	private EnumMap<LockMode,JdbcSelect> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<LoadQueryInfluencers.InternalFetchProfileType,JdbcSelect> selectByInternalCascadeProfile;

	public CollectionLoaderImpl(PluralPersistentAttribute pluralAttribute, SessionFactoryImplementor sessionFactory) {
		this.pluralAttribute = pluralAttribute;

		this.selectionBuilder = new SelectByCollectionKeyBuilder(
				pluralAttribute.getPersistentCollectionDescriptor(),
				sessionFactory
		);
	}

	@Override
	public Navigable getLoadedNavigable() {
		return pluralAttribute;
	}

	@Override
	public PersistentCollection load(Object key, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final ParameterBindingContext parameterBindingContext = new LoadParameterBindingContext(
				session.getFactory(),
				key
		);

		final JdbcSelect jdbcSelect = resolveJdbcSelect( lockOptions, session );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();

		pluralAttribute.getPersistentCollectionDescriptor().getCollectionKeyDescriptor().dehydrate(
				key,
				new Writeable.JdbcValueCollector() {
					private int count = 0;

					@Override
					public void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn) {
						jdbcParameterBindings.addBinding(
								new StandardJdbcParameterImpl(
										count++,
										type,
										Clause.WHERE,
										session.getFactory().getTypeConfiguration()
								),
								new JdbcParameterBinding() {
									@Override
									public SqlExpressableType getBindType() {
										return type;
									}

									@Override
									public Object getBindValue() {
										return jdbcValue;
									}
								}
						);
					}
				},
				Clause.WHERE,
				session
		);

		JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public ParameterBindingContext getParameterBindingContext() {
						return parameterBindingContext;
					}

					@Override
					public JdbcParameterBindings getJdbcParameterBindings() {
						return jdbcParameterBindings;
					}

					@Override
					public Callback getCallback() {
						return null;
					}
				},
				RowTransformerSingularReturnImpl.instance()
		);

		final CollectionKey collectionKey = new CollectionKey(
				pluralAttribute.getPersistentCollectionDescriptor(),
				key
		);

		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private JdbcSelect resolveJdbcSelect(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		if ( pluralAttribute.getPersistentCollectionDescriptor().isAffectedByEnabledFilters( session ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even "internal" fetch profiles.
			return createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() );
		}

		if ( loadQueryInfluencers.getEnabledInternalFetchProfileType() != null ) {
			if ( LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( selectByInternalCascadeProfile == null ) {
					selectByInternalCascadeProfile = new EnumMap<>( LoadQueryInfluencers.InternalFetchProfileType.class );
				}
				return selectByInternalCascadeProfile.computeIfAbsent(
						loadQueryInfluencers.getEnabledInternalFetchProfileType(),
						internalFetchProfileType -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() )
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, session );

		if ( cacheable ) {
			return selectByLockMode.computeIfAbsent(
					lockOptions.getLockMode(),
					lockMode -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() )
			);
		}

		return createJdbcSelect(
				lockOptions,
				loadQueryInfluencers,
				session.getSessionFactory()
		);

	}

	private JdbcSelect createJdbcSelect(
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {

		final SqlAstSelectDescriptor selectDescriptor = selectionBuilder.generateSelectStatement(
				1,
				queryInfluencers,
				lockOptions
		);


		return SqlAstSelectToJdbcSelectConverter.interpret(
				selectDescriptor,
				sessionFactory
		);
	}


	@SuppressWarnings("RedundantIfStatement")
	private boolean determineIfCacheable(LockOptions lockOptions, SharedSessionContractImplementor session) {
		if ( pluralAttribute.getPersistentCollectionDescriptor().isAffectedByEnabledFilters( session ) ) {
			return false;
		}

		if ( lockOptions.getTimeOut() == LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}
}
