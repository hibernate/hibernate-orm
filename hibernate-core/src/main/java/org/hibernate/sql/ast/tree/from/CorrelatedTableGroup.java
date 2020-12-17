/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * A virtual {@link TableReference} for correlated roots.
 * Table group joins are pushed into the from clause as roots and join predicates to the where clause.
 *
 * @author Christian Beikov
 */
public class CorrelatedTableGroup extends AbstractTableGroup {

	private final TableGroup correlatedTableGroup;
	private final QuerySpec querySpec;
	private final Consumer<Predicate> joinPredicateConsumer;

	public CorrelatedTableGroup(
			TableGroup correlatedTableGroup,
			SqlAliasBase sqlAliasBase,
			QuerySpec querySpec,
			Consumer<Predicate> joinPredicateConsumer,
			SessionFactoryImplementor sessionFactory) {
		super(
				correlatedTableGroup.getNavigablePath(),
				(TableGroupProducer) correlatedTableGroup.getExpressionType(),
				LockMode.NONE,
				sqlAliasBase,
				sessionFactory
		);
		this.correlatedTableGroup = correlatedTableGroup;
		this.querySpec = querySpec;
		this.joinPredicateConsumer = joinPredicateConsumer;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( getTableGroupJoins().contains( join ) ) {
			return;
		}
		assert join.getJoinType() == SqlAstJoinType.INNER;
		querySpec.getFromClause().addRoot( join.getJoinedGroup() );
		joinPredicateConsumer.accept( join.getPredicate() );
		super.addTableGroupJoin( join );
	}

	@Override
	protected TableReference getTableReferenceInternal(String tableExpression) {
		final TableReference primaryTableReference = correlatedTableGroup.getPrimaryTableReference();
		if ( tableExpression.equals( primaryTableReference.getTableExpression() ) ) {
			return primaryTableReference;
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup().getPrimaryTableReference();
			if ( groupTableReference.getTableReference( tableExpression ) != null ) {
				return groupTableReference;
			}
		}
		for ( TableReferenceJoin tableReferenceJoin : correlatedTableGroup.getTableReferenceJoins() ) {
			if ( tableExpression.equals( tableReferenceJoin.getJoinedTableReference().getTableExpression() ) ) {
				return tableReferenceJoin.getJoinedTableReference();
			}
		}
		return null;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( getPrimaryTableReference().getTableExpression() );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return correlatedTableGroup.getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
