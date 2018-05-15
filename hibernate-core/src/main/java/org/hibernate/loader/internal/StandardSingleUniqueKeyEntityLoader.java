/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.internal;

import java.util.EnumMap;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByUniqueKeyBuilder;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Andrea Boriero
 */
public class StandardSingleUniqueKeyEntityLoader<T> implements SingleUniqueKeyEntityLoader<T> {
	private final SingularPersistentAttributeEntity attribute;
	private final Navigable fkTargetAttribute;

	private EnumMap<LockMode, JdbcSelect> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<LoadQueryInfluencers.InternalFetchProfileType, JdbcSelect> selectByInternalCascadeProfile;


	public StandardSingleUniqueKeyEntityLoader(Navigable fkTargetAttribute, SingularPersistentAttributeEntity attribute) {
		this.attribute = attribute;
		this.fkTargetAttribute = fkTargetAttribute;

		// todo (6.0) : selectByLockMode and selectByInternalCascadeProfile
	}

	@Override
	public EntityTypeDescriptor getLoadedNavigable() {
		return attribute.getAssociatedEntityDescriptor();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T load(
			Object uk,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		if ( uk == null ) {
			return null;
		}

		final EntityUniqueKey pcKey = new EntityUniqueKey(
				getLoadedNavigable().getEntityName(),
				fkTargetAttribute.getNavigableName(),
				uk,
				getLoadedNavigable().getIdentifierDescriptor().getJavaTypeDescriptor(),
				fkTargetAttribute.getJavaTypeDescriptor(),
				getLoadedNavigable().getHierarchy().getRepresentation(),
				getLoadedNavigable().getFactory()
		);


		// is there already one associated with the Session?  Use it..
		final Object existingEntityInstance = session.getPersistenceContext().getEntity( pcKey );
		if ( existingEntityInstance != null ) {
			return (T) existingEntityInstance;
		}

		return loadEntity( uk, lockOptions, session );
	}

	private T loadEntity(Object uk, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final JdbcSelect jdbcSelect = resolveJdbcSelect( lockOptions, session );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();

		// todo (6.0) : this is not always a
		final SingularPersistentAttributeEntity attribute = (SingularPersistentAttributeEntity) fkTargetAttribute;

		attribute.dehydrate(
				uk,
				(jdbcValue, type, boundColumn) -> {
					jdbcParameterBindings.addBinding(
							new StandardJdbcParameterImpl(
									jdbcParameterBindings.getBindings().size(),
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
				},
				Clause.WHERE,
				session
		);

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				getExecutionContext( session, jdbcParameterBindings ),
				RowTransformerSingularReturnImpl.instance()
		);

		if ( list.isEmpty() ) {
			return null;
		}

		return list.get( 0 );
	}

	private JdbcSelect resolveJdbcSelect(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( getLoadedNavigable().isAffectedByEnabledFilters( session ) ) {
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
						internalFetchProfileType -> createJdbcSelect(
								lockOptions,
								loadQueryInfluencers,
								session.getSessionFactory()
						)
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, loadQueryInfluencers );

		if ( cacheable ) {
			return selectByLockMode.computeIfAbsent(
					lockOptions.getLockMode(),
					lockMode -> createJdbcSelect(
							lockOptions,
							loadQueryInfluencers,
							session.getSessionFactory()
					)
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

		final SelectByUniqueKeyBuilder selectBuilder = new SelectByUniqueKeyBuilder(
				sessionFactory,
				getLoadedNavigable(),
				attribute
		);

		final SqlAstSelectDescriptor selectDescriptor = selectBuilder.generateSelectStatement(
				1,
				queryInfluencers,
				lockOptions
		);

		return SqlAstSelectToJdbcSelectConverter.interpret( selectDescriptor, sessionFactory );
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean determineIfCacheable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( getLoadedNavigable().isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( lockOptions.getTimeOut() == LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}

	private ExecutionContext getExecutionContext(
			SharedSessionContractImplementor session,
			JdbcParameterBindings jdbcParameterBindings) {
		final ParameterBindingContext parameterBindingContext = new TemplateParameterBindingContext( session.getFactory() );

		return new ExecutionContext() {
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
		};
	}
}
