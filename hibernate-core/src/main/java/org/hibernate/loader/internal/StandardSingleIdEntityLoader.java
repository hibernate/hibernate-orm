/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.internal.LoadIdParameter;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByEntityIdentifierBuilder;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.LoadParameterBindingContext;
import org.hibernate.sql.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class StandardSingleIdEntityLoader<T> implements SingleIdEntityLoader<T> {
	private final EntityTypeDescriptor<T> entityDescriptor;

	private final SqlAstSelectDescriptor databaseSnapshotSelectAst;
	private LoadIdParameter idParameter;

	private EnumMap<LockMode,JdbcSelect> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<InternalFetchProfileType,JdbcSelect> selectByInternalCascadeProfile;

	public StandardSingleIdEntityLoader(EntityTypeDescriptor<T> entityDescriptor) {
		this.entityDescriptor = entityDescriptor;

		this.databaseSnapshotSelectAst = generateDatabaseSnapshotSelect( entityDescriptor );

// todo (6.0) : re-enable this pre-caching after model processing is more fully complete
//		ParameterBindingContext context = new TemplateParameterBindingContext( entityDescriptor.getFactory(), 1 );
//		final JdbcSelect base = createJdbcSelect( LockOptions.READ, LoadQueryInfluencers.NONE, context );
//
//		selectByLockMode.put( LockMode.NONE, base );
//		selectByLockMode.put( LockMode.READ, base );
	}

	@Override
	public EntityTypeDescriptor<T> getLoadedNavigable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object id, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final ParameterBindingContext parameterBindingContext = new LoadParameterBindingContext(
				session.getFactory(),
				id
		);

		final JdbcSelect jdbcSelect = resolveJdbcSelect(
				lockOptions,
				session
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		entityDescriptor.getHierarchy().getIdentifierDescriptor().dehydrate(
				entityDescriptor.getHierarchy().getIdentifierDescriptor().unresolve( id, session ),
//				id,
				new ExpressableType.JdbcValueCollector() {
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

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
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

		if ( list.isEmpty() ) {
			return null;
		}

		final T entityInstance = list.get( 0 );
		return entityInstance;
	}

	private JdbcSelect resolveJdbcSelect(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( entityDescriptor.isAffectedByEnabledFilters( session ) ) {
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
					selectByInternalCascadeProfile = new EnumMap<>( InternalFetchProfileType.class );
				}
				return selectByInternalCascadeProfile.computeIfAbsent(
						loadQueryInfluencers.getEnabledInternalFetchProfileType(),
						internalFetchProfileType -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() )
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, loadQueryInfluencers );

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
		final SelectByEntityIdentifierBuilder selectBuilder = new SelectByEntityIdentifierBuilder(
				entityDescriptor.getFactory(),
				entityDescriptor
		);
		final SqlAstSelectDescriptor selectDescriptor = selectBuilder
				.generateSelectStatement( 1, queryInfluencers, lockOptions );


		return SqlAstSelectToJdbcSelectConverter.interpret(
				selectDescriptor,
				sessionFactory
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean determineIfCacheable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( entityDescriptor.isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {

		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				databaseSnapshotSelectAst,
				session.getSessionFactory()
		);

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				getExecutionContext( id, session ),
				RowTransformerPassThruImpl.instance()
		);

		if ( list.isEmpty() ) {
			return null;
		}

		return (Object[]) list.get( 0 );
	}

	private ExecutionContext getExecutionContext(Object id,SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
			entityDescriptor.getHierarchy().getIdentifierDescriptor().dehydrate(
					id,
					(jdbcValue, type, boundColumn) -> jdbcParameterBindings.addBinding(
							idParameter,
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
					),
					Clause.WHERE,
					session
			);

		final ParameterBindingContext parameterBindingContext = new ParameterBindingContext() {
			@Override
			public <T> List<T> getLoadIdentifiers() {
				return Collections.emptyList();
			}

			@Override
			public QueryParameterBindings getQueryParameterBindings() {
				return QueryParameterBindings.NO_PARAM_BINDINGS;
			}

			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return session.getSessionFactory();
			}
		};

		return new ExecutionContext(){

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
			public Callback getCallback() {
				return afterLoadAction -> {
				};
			}

			@Override
			public JdbcParameterBindings getJdbcParameterBindings() {
				return jdbcParameterBindings;
			}
		};
	}

	private SqlAstSelectDescriptor generateDatabaseSnapshotSelect(EntityTypeDescriptor<?> entityDescriptor) {
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );
		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();


		final TableSpace rootTableSpace = rootQuerySpec.getFromClause().makeTableSpace();

		final SqlAliasBaseGenerator aliasBaseGenerator = new SqlAliasBaseManager();

		final NavigablePath path = new NavigablePath( entityDescriptor.getEntityName() );
		final EntityTableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return "root";
					}

					@Override
					public String getIdentificationVariable() {
						return null;
					}

					@Override
					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
						return entityDescriptor;
					}

					@Override
					public NavigablePath getNavigablePath() {
						return path;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						rootQuerySpec.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return rootQuerySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return rootTableSpace;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return aliasBaseGenerator;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return null;
					}

					@Override
					public LockOptions getLockOptions() {
						return LockOptions.NONE;
					}
				}
		);

		rootTableSpace.setRootTableGroup( rootTableGroup );

		final List<DomainResult> domainResults = new ArrayList<>();

		EntityIdentifier<Object, Object> identifierDescriptor = entityDescriptor.getHierarchy()
				.getIdentifierDescriptor();

		final List<ColumnReference> columnReferences = new ArrayList();
		identifierDescriptor.visitColumns(
				(sqlExpressableType, column) -> {
					final Expression expression = rootTableGroup.qualify( column );
					ColumnReference idColumnReference;
					if ( !ColumnReference.class.isInstance( expression ) ) {
						idColumnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
					}
					else {
						idColumnReference = (ColumnReference) expression;
					}
					columnReferences.add( idColumnReference );
					SqlSelection sqlSelection = new SqlSelectionImpl(
							1,
							0,
							idColumnReference,
							sqlExpressableType.getJdbcValueExtractor()
					);
					selectClause.addSqlSelection( sqlSelection );

					domainResults.add( new BasicResultImpl(
							null,
							sqlSelection,
							sqlExpressableType
					) );
				},
				Clause.SELECT,
				null
		);

		final Expression idExpression;

		if ( columnReferences.size() == 1 ) {
			idExpression = columnReferences.get( 0 );
		}
		else {
			idExpression = new SqlTuple( columnReferences );
		}

		// todo (6.0) : is this correct and/or is theew a better way to add the SqlSelection to the SelectClause?
		entityDescriptor.visitStateArrayContributors(
				stateArrayContributor ->
						stateArrayContributor.visitColumns(
								(sqlExpressableType, column) -> {
									ColumnReference columnReference;
									final Expression expression = rootTableGroup.qualify( column );

									if ( !ColumnReference.class.isInstance( expression ) ) {
										columnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
									}
									else {
										columnReference = (ColumnReference) expression;
									}
									int stateArrayPosition = stateArrayContributor.getStateArrayPosition();
									SqlSelection sqlSelection = new SqlSelectionImpl(
											stateArrayPosition + 2,
											stateArrayPosition +1,
											columnReference,
											sqlExpressableType.getJdbcValueExtractor()
									);

									selectClause.addSqlSelection( sqlSelection );
									domainResults.add( new BasicResultImpl(
											null,
											sqlSelection,
											sqlExpressableType
									) );
								},
								Clause.SELECT,
								null
						)
		);

		idParameter = new LoadIdParameter(
				identifierDescriptor,
				entityDescriptor.getFactory().getTypeConfiguration()
		);
		rootQuerySpec.addRestriction(
				new RelationalPredicate(
						RelationalPredicate.Operator.EQUAL,
						idExpression,
						idParameter
				)
		);

		return new SqlAstSelectDescriptorImpl(
				selectStatement,
				domainResults,
				entityDescriptor.getAffectedTableNames()
		);
	}


}
