/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
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
			DomainResultCreationState creationState) {
		final CollectionElement elementDescriptor = getCollectionDescriptor().getElementDescriptor();

		final NavigablePath collectionNavigablePath = tableGroup.getNavigablePath();

		final DomainResult domainResult = elementDescriptor.createDomainResult(
				collectionNavigablePath.append( CollectionElement.NAVIGABLE_NAME ),
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
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
		domainResultsCollector.accept( domainResult );

	}

	@Override
	protected SqlAstCreationStateImpl getCreationState(
			SessionFactoryImplementor sessionFactory, QuerySpec querySpec) {
		return new IndexSqlAstCreationStateImpl( sessionFactory, querySpec );
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
			BiConsumer<Column, JdbcParameter> columnCollector,
			SqlAstCreationState creationState) {
		applyPredicates(
				junction,
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				tableGroup,
				columnCollector,
				creationState
		);
		applyPredicates(
				junction,
				getCollectionDescriptor().getIndexDescriptor(),
				tableGroup,
				columnCollector,
				creationState
		);
	}

	public class IndexSqlAstCreationStateImpl extends SqlAstCreationStateImpl {

		public IndexSqlAstCreationStateImpl(
				SessionFactoryImplementor sessionFactory,
				QuerySpec querySpec) {
			super( sessionFactory, querySpec );
		}

		@Override
		public List<Fetch> visitFetches(FetchParent fetchParent) {
			final List<Fetch> fetches = new ArrayList<>();
			final Consumer<Fetchable> fetchableConsumer = fetchable -> {
				if ( fetchParent.findFetch( fetchable.getNavigableName() ) != null ) {
					return;
				}

				fetches.add(
						fetchable.generateFetch(
								fetchParent,
								FetchTiming.IMMEDIATE,
								true,
								LockMode.NONE,
								null,
								this
						)
				);
			};

			NavigableContainer navigableContainer = (NavigableContainer) getCollectionDescriptor().getElementDescriptor();
			navigableContainer.visitKeyFetchables( fetchableConsumer );
			navigableContainer.visitFetchables( fetchableConsumer );

			return fetches;
		}
	}
}
