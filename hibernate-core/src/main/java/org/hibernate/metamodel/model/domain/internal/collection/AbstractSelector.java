/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 *
 * todo (6.0) : is it an AbstractSelector or an AbstractExecutor or .... ?
 */
public abstract class AbstractSelector {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final JdbcSelect jdbcSelect;
	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final List<JdbcParameterBinder> jdbcParameterBinders;

	public AbstractSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table table,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.jdbcParameterMap = new HashMap<>();
		this.jdbcParameterBinders = new ArrayList<>();

		this.jdbcSelect = generateSelect(
				table,
				sqlWhereString,
				jdbcParameterBinders::add,
				jdbcParameterMap::put,
				sessionFactory
		);
	}

	protected List execute(JdbcParameterBindings jdbcParameterBindings, SharedSessionContractImplementor session) {
		final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				executionContext,
				RowTransformerSingularReturnImpl.INSTANCE
		);
	}

	protected PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	protected abstract void applySqlSelections(
			QuerySpec querySpec,
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			SessionFactoryImplementor sessionFactory);

	protected abstract void applyPredicates(
			Junction junction,
			TableGroup tableGroup,
			Consumer<JdbcParameterBinder> jdbcParameterBinder,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory);

	protected <T> void applyPredicates(
			Junction junction,
			Navigable<T> navigable,
			TableGroup tableGroup,
			Consumer<JdbcParameterBinder> jdbcParameterBinder,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		final AtomicInteger parameterCount = new AtomicInteger();
		navigable.visitColumns(
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
					junction.add( new ComparisonPredicate(
							columnReference,
							ComparisonOperator.EQUAL,
							parameter
					) );
				},
				Clause.WHERE,
				sessionFactory.getTypeConfiguration()
		);
	}

	protected void bindCollectionIndex(
			Object index,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		bindValue( index, getCollectionDescriptor().getIndexDescriptor(), jdbcParameterBindings, session );
	}

	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		bindValue( key, getCollectionDescriptor().getCollectionKeyDescriptor(), jdbcParameterBindings, session );
	}

	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		CollectionElement elementDescriptor = getCollectionDescriptor().getElementDescriptor();

		Object unresolved = elementDescriptor.unresolve(
				entry,
				session
		);

		bindValue( unresolved, elementDescriptor, jdbcParameterBindings, session );
	}

	private void bindValue(
			Object value,
			Navigable navigable,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		navigable.dehydrate(
				value,
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.WHERE,
				session
		);
	}

	private JdbcSelect generateSelect(
			Table table,
			String sqlWhereString,
			Consumer<JdbcParameterBinder> jdbcParameterCollector,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );

		final TableGroup rootTableGroup = createTableGroup( rootQuerySpec, sessionFactory );
		rootQuerySpec.getFromClause().addRoot( rootTableGroup );

		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();
		final List<DomainResult> domainResults = new ArrayList<>();

		applySqlSelections(
				rootQuerySpec,
				rootTableGroup,
				selectClause,
				domainResults::add,
				sessionFactory
		);

		Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		applyPredicates(
				predicate,
				rootTableGroup,
				jdbcParameterCollector,
				columnCollector,
				sessionFactory
		);

		rootQuerySpec.addRestriction( predicate );
		applyWhereFragment( sqlWhereString, rootQuerySpec );

		final Set<String> affectedTableNames = new HashSet<>();
		affectedTableNames.add( table.getTableExpression() );
		return SqlAstSelectToJdbcSelectConverter.interpret(
				new SqlAstSelectDescriptorImpl(
						selectStatement,
						domainResults,
						affectedTableNames
				),
				sessionFactory
		);
	}

	private TableGroup createTableGroup(
			QuerySpec querySpec,
			SessionFactoryImplementor sessionFactory) {

		final SqlAstCreationState creationState = new SqlAstCreationState() {
			final SqlAliasBaseGenerator aliasBaseGenerator = new SqlAliasBaseManager();
			final SqlAstProcessingState processingState = new SqlAstQuerySpecProcessingStateImpl(
					querySpec,
					null,
					this,
					() -> null,
					() -> expression -> {},
					() -> sqlSelection -> {}
			);

			@Override
			public SqlAstCreationContext getCreationContext() {
				return sessionFactory;
			}

			@Override
			public SqlAstProcessingState getCurrentProcessingState() {
				return processingState;
			}

			@Override
			public SqlExpressionResolver getSqlExpressionResolver() {
				return processingState.getSqlExpressionResolver();
			}

			@Override
			public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
				return aliasBaseGenerator;
			}

			@Override
			public LockMode determineLockMode(String identificationVariable) {
				return LockMode.NONE;
			}

			@Override
			public List<Fetch> visitFetches(FetchParent fetchParent) {
				return Collections.emptyList();
			}
		};

		return collectionDescriptor.createRootTableGroup(
				null,
				new NavigablePath( collectionDescriptor.getNavigableRole().getFullPath() ),
				null,
				null,
				LockMode.NONE,
				creationState
		);
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

}
