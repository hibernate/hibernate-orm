/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.internal.PerQuerySpecSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Andrea Boriero
 */
public class JoinTableCollectionRowByIndexSelector extends AbstractSelector implements CollectionRowByIndexSelector {

	public <E, O, C> JoinTableCollectionRowByIndexSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table table,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		super( collectionDescriptor, table, sqlWhereString, sessionFactory );
	}

	@Override
	public Object execute(Object key, Object index, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		bindCollectionKey( key, jdbcParameterBindings, session );
		bindCollectionIndex( index, jdbcParameterBindings, session );
		List results = execute( jdbcParameterBindings, session );
		if ( results.isEmpty() ) {
			return null;
		}
		return results.get( 0 );
	}

	@Override
	protected void applySqlSelections(
			QuerySpec querySpec,
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			SessionFactoryImplementor sessionFactory) {
		final CollectionElement elementDescriptor = getCollectionDescriptor().getElementDescriptor();

		final NavigablePath collectionNavigablePath = tableGroup.getNavigablePath();

		final CreationState creationState = new CreationState( sessionFactory, querySpec );

		final DomainResult domainResult = elementDescriptor.createDomainResult(
				collectionNavigablePath.append( elementDescriptor.getNavigableName() ),
				null,
				creationState
		);

		final Position position = new Position();
		elementDescriptor.visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {
					final Expression expression = tableGroup.qualify( column );
					ColumnReference columnReference;
					if ( !ColumnReference.class.isInstance( expression ) ) {
						columnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
					}
					else {
						columnReference = (ColumnReference) expression;
					}
					SqlSelection sqlSelection = new SqlSelectionImpl(
							position.getJdbcPosition(),
							position.getValuesArrayPosition(),
							columnReference,
							sqlExpressableType.getJdbcValueExtractor()
					);
					position.increase();
					selectClause.addSqlSelection( sqlSelection );
				},
				Clause.SELECT,
				sessionFactory.getTypeConfiguration()
		);
		domainResultsCollector.accept( domainResult );

	}

	public class Position {
		int jdbcPosition = 1;
		int valuesArrayPosition = 0;

		public void increase() {
			jdbcPosition++;
			valuesArrayPosition++;
		}

		public int getJdbcPosition() {
			return jdbcPosition;
		}

		public int getValuesArrayPosition() {
			return valuesArrayPosition;
		}
	}

	@Override
	protected void applyPredicates(
			Junction junction,
			TableGroup tableGroup,
			Consumer<JdbcParameterBinder> jdbcParameterBinder,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		applyPredicates(
				junction,
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				tableGroup,
				jdbcParameterBinder,
				columnCollector,
				sessionFactory
		);
		applyPredicates(
				junction,
				getCollectionDescriptor().getIndexDescriptor(),
				tableGroup,
				jdbcParameterBinder,
				columnCollector,
				sessionFactory
		);
	}


	private class CreationState implements DomainResultCreationState, SqlAstCreationState {
		private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
		private final DomainResultCreationState.FromClauseAccess fromClauseAccess = new DomainResultCreationState.SimpleFromClauseAccessImpl();

		private final PerQuerySpecSqlExpressionResolver sqlExpressionResolver;
		private final SessionFactoryImplementor sessionFactory;

		public CreationState(SessionFactoryImplementor sessionFactory, QuerySpec querySpec) {
			this.sessionFactory = sessionFactory;
			this.sqlExpressionResolver = new PerQuerySpecSqlExpressionResolver(
					sessionFactory,
					() -> querySpec,
					expression -> expression,
					(expression, selection) -> {
					}
			);
		}

		@Override
		public SqlAstCreationContext getCreationContext() {
			return sessionFactory;
		}

		@Override
		public SqlExpressionResolver getSqlExpressionResolver() {
			return sqlExpressionResolver;
		}

		@Override
		public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
			return sqlAliasBaseManager;
		}

		@Override
		public SqlAstCreationState getSqlAstCreationState() {
			return null;
		}

		@Override
		public List<Fetch> visitFetches(FetchParent fetchParent) {
			return Collections.emptyList();
		}

		@Override
		public FromClauseAccess getFromClauseAccess() {
			return fromClauseAccess;
		}

		@Override
		public boolean fetchAllAttributes() {
			return false;
		}

		@Override
		public TableSpace getCurrentTableSpace() {
			throw new UnsupportedOperationException(  );
		}

		@Override
		public LockMode determineLockMode(String identificationVariable) {
			return null;
		}
	}
}
