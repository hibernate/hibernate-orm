/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
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
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
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

	public AbstractSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table table,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.jdbcParameterMap = new HashMap<>();

		this.jdbcSelect = generateSelect(
				table,
				sqlWhereString,
				jdbcParameterMap::put,
				sessionFactory
		);
	}

	protected PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	protected List execute(JdbcParameterBindings jdbcParameterBindings, SharedSessionContractImplementor session) {
		final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				executionContext,
				RowTransformerSingularReturnImpl.INSTANCE
		);
	}

	private JdbcSelect generateSelect(
			Table table,
			String sqlWhereString,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {

		final QuerySpec querySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( querySpec );

		final SqlAstCreationStateImpl creationState = getCreationState( sessionFactory, querySpec );

		final TableGroup rootTableGroup = createTableGroup( creationState );

		querySpec.getFromClause().addRoot( rootTableGroup );
		creationState.getFromClauseAccess().registerTableGroup( rootTableGroup.getNavigablePath(), rootTableGroup );

		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();
		final List<DomainResult> domainResults = new ArrayList<>();

		applySqlSelections(
				querySpec,
				rootTableGroup,
				selectClause,
				domainResults::add,
				creationState
		);

		Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		applyPredicates(
				predicate,
				rootTableGroup,
				columnCollector,
				creationState
		);

		querySpec.addRestriction( predicate );
		applyWhereFragment( sqlWhereString, querySpec );

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

	protected SqlAstCreationStateImpl getCreationState(SessionFactoryImplementor sessionFactory, QuerySpec querySpec) {
		return new SqlAstCreationStateImpl( sessionFactory, querySpec );
	}

	private TableGroup createTableGroup(SqlAstCreationStateImpl creationState) {

		return collectionDescriptor.createRootTableGroup(
				new NavigablePath( collectionDescriptor.getNavigableRole().getFullPath() ),
				null,
				null,
				LockMode.NONE,
				creationState
		);
	}

	protected abstract void applySqlSelections(
			QuerySpec querySpec,
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			DomainResultCreationState creationState);

	protected abstract void applyPredicates(
			Junction junction,
			TableGroup tableGroup,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SqlAstCreationState creationState);

	protected <T> void applyPredicates(
			Junction junction,
			Navigable<T> navigable,
			TableGroup tableGroup,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SqlAstCreationState creationState) {
		final TypeConfiguration typeConfiguration = creationState.getCreationContext()
				.getDomainModel()
				.getTypeConfiguration();
		final AtomicInteger parameterCount = new AtomicInteger();
		navigable.visitColumns(
				(sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.WHERE,
							typeConfiguration
					);

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
				typeConfiguration
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
