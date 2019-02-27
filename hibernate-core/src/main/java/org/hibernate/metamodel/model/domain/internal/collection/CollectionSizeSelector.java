/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Chris Cranford
 */
public class CollectionSizeSelector {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final boolean integerIndexed;
	private final JdbcSelect jdbcSelect;
	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final List<JdbcParameterBinder> jdbcParameterBinders;

	public CollectionSizeSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			boolean isIntegerIndexed,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.integerIndexed = isIntegerIndexed;
		this.jdbcParameterMap = new HashMap<>();
		this.jdbcParameterBinders = new ArrayList<>();

		final TableReference tableReference = new TableReference( dmlTargetTable, null, false );
		this.jdbcSelect = generateSizeSelect(
				sqlWhereString,
				jdbcParameterBinders::add,
				jdbcParameterMap::put,
				sessionFactory
		);
	}

	@SuppressWarnings("unchecked")
	public int execute(Object key, SharedSessionContractImplementor session) {
		final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
		final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

		bindCollectionKey( key, jdbcParameterBindings, session );

		List results = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				executionContext,
				RowTransformerSingularReturnImpl.INSTANCE
		);

		if ( results.size() != 1 ) {
			return 0;
		}

		Object result = results.get( 0 );
		if ( result == null ) {
			return 0;
		}

		return (int) result + 1;
	}

	private void bindCollectionKey(
			Object key,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				key,
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						boundColumn.getExpressableType(),
						jdbcParameterBindings,
						session
				),
				Clause.WHERE,
				session
		);
	}

	private void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.INSERT,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	private JdbcParameter resolveJdbcParameter(Column boundColumn) {
		final JdbcParameter jdbcParameter = jdbcParameterMap.get( boundColumn );
		if ( jdbcParameter == null ) {
			throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
		}
		return jdbcParameter;
	}

	private JdbcSelect generateSizeSelect(
			String sqlWhereString,
			Consumer<JdbcParameterBinder> jdbcParameterCollector,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );
		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();
		final Set<String> affectedTableNames = new HashSet<>();

		final TableSpace rootTableSpace = rootQuerySpec.getFromClause().makeTableSpace();

		final TableGroup rootTableGroup = resolveTableGroup( rootQuerySpec, rootTableSpace, affectedTableNames::addAll );
		rootTableSpace.setRootTableGroup( rootTableGroup );

		final List<DomainResult> domainResults = new ArrayList<>();

		applySqlSelections(
				rootTableGroup,
				selectClause,
				domainResults::add,
				sessionFactory
		);

		applyPredicates(
				rootQuerySpec,
				rootTableGroup,
				sqlWhereString,
				jdbcParameterCollector,
				columnCollector,
				sessionFactory
		);

		return SqlAstSelectToJdbcSelectConverter.interpret(
				new SqlAstSelectDescriptorImpl(
						selectStatement,
						domainResults,
						affectedTableNames
				),
				sessionFactory
		);
	}

	private TableGroup resolveTableGroup(
			QuerySpec querySpec,
			TableSpace tableSpace,
			Consumer<Collection<String>> tableNameCollector) {

		final SqlAliasBaseGenerator aliasBaseGenerator = new SqlAliasBaseManager();

		if ( !( collectionDescriptor.getElementDescriptor() instanceof CollectionElementEntity ) ) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		final CollectionElementEntity<?> elementDescriptor = (CollectionElementEntity) collectionDescriptor.getElementDescriptor();
		final EntityTypeDescriptor<?> entityDescriptor = elementDescriptor.getEntityDescriptor();
		final NavigablePath navigablePath = new NavigablePath( entityDescriptor.getEntityName() );

		tableNameCollector.accept( entityDescriptor.getAffectedTableNames() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
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
						return navigablePath;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						querySpec.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return tableSpace;
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

		return tableGroup;
	}

	private void applySqlSelections(
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			SessionFactoryImplementor sessionFactory) {
		if ( integerIndexed ) {
			// Build selection of "max(indexColumn[0])"
			final List<Column> indexColumns = collectionDescriptor.getIndexDescriptor().getColumns();
			final Column column = indexColumns.get( 0 );

			final Expression columnExpression = tableGroup.qualify( column );

			final Expression maxExpression = new MaxFunction(
					columnExpression,
					false,
					column.getExpressableType()
			);

			final SqlExpressableType sqlExpressableType = IntegerSqlDescriptor.INSTANCE.getSqlExpressableType(
					IntegerJavaDescriptor.INSTANCE,
					sessionFactory.getTypeConfiguration()
			);

			SqlSelection sqlSelection = new SqlSelectionImpl(
					1,
					0,
					maxExpression,
					column.getExpressableType().getJdbcValueExtractor()
			);

			selectClause.addSqlSelection( sqlSelection );

			domainResultsCollector.accept(
					new BasicResultImpl(
							null,
							sqlSelection,
							indexColumns.get( 0 ).getExpressableType()
					)
			);
		}
		else {
			// Build selection of "count(1)"
			final SqlExpressableType sqlExpressableType = IntegerSqlDescriptor.INSTANCE.getSqlExpressableType(
					IntegerJavaDescriptor.INSTANCE,
					sessionFactory.getTypeConfiguration()
			);

			final Expression countExpression = new CountFunction(
					new QueryLiteral(
							1,
							sqlExpressableType,
							Clause.SELECT
					),
					false,
					sqlExpressableType
			);

			SqlSelection sqlSelection = new SqlSelectionImpl(
					1,
					0,
					countExpression,
					sqlExpressableType.getJdbcValueExtractor()
			);

			selectClause.addSqlSelection( sqlSelection );

			domainResultsCollector.accept(
					new BasicResultImpl(
							null,
							sqlSelection,
							sqlExpressableType
					)
			);
		}
	}

	private void applyPredicates(
			QuerySpec querySpec,
			TableGroup tableGroup,
			String sqlWhereString,
			Consumer<JdbcParameterBinder> jdbcParameterBinder,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		final AtomicInteger parameterCount = new AtomicInteger();

		final CollectionKey<?> collectionKey = collectionDescriptor.getCollectionKeyDescriptor();
		collectionKey.visitColumns(
				(sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.WHERE,
							sessionFactory.getTypeConfiguration()
					);

					jdbcParameterBinder.accept( parameter );
					columnCollector.accept( column, parameter );

					final Expression expression = tableGroup.qualify( column );
					final ColumnReference columnReference;
					if ( !( expression instanceof ColumnReference ) ) {
						columnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
					}
					else {
						columnReference = (ColumnReference) expression;
					}

					querySpec.addRestriction(
							new ComparisonPredicate(
									columnReference,
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.WHERE,
				sessionFactory.getTypeConfiguration()
		);

		applyWhereFragment( sqlWhereString, querySpec );
	}

	private void applyWhereFragment(String sqlWhereString, QuerySpec querySpec) {
		if ( StringHelper.isNotEmpty( sqlWhereString ) ) {
			final SelfRenderingExpression whereExpression = new SelfRenderingExpression() {
				@Override
				public void renderToSql(
						SqlAppender sqlAppender,
						SqlAstWalker walker,
						SessionFactoryImplementor sessionFactory) {
					sqlAppender.appendSql( sqlWhereString );
				}

				@Override
				public SqlExpressableType getType() {
					return null;
				}

				@Override
				public SqlSelection createSqlSelection(
						int jdbcPosition,
						int valuesArrayPosition,
						BasicJavaDescriptor javaTypeDescriptor,
						TypeConfiguration typeConfiguration) {
					return null;
				}
			};

			final SelfRenderingPredicate wherePredicate = new SelfRenderingPredicate( whereExpression );
			querySpec.addRestriction( wherePredicate );
		}
	}
}
