package org.hibernate.sql.ast.spi.query.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;

/**
 * A virtual {@link TableReference} for correlated roots.
 * Inner joins become subquery roots with their predicates in the where clause.
 * Left joins need a single row on their left to preserve missing matches.
 *
 * @author Christian Beikov
 */
public class CorrelatedTableGroup extends AbstractTableGroup {
	private final TableGroup correlatedTableGroup;
	private final QuerySpec querySpec;
	private final Consumer<Predicate> joinPredicateConsumer;
	private TableGroup singleRowGroup;

	public CorrelatedTableGroup(
			TableGroup correlatedTableGroup,
			SqlAliasBase sqlAliasBase,
			QuerySpec querySpec,
			Consumer<Predicate> joinPredicateConsumer,
			SessionFactoryImplementor sessionFactory) {
		super(
				true,
				correlatedTableGroup.getNavigablePath(),
				(TableGroupProducer) correlatedTableGroup.getExpressionType(),
				null,
				sqlAliasBase,
				sessionFactory
		);
		this.correlatedTableGroup = correlatedTableGroup;
		this.querySpec = querySpec;
		this.joinPredicateConsumer = joinPredicateConsumer;
	}

	public TableGroup getCorrelatedTableGroup() {
		return correlatedTableGroup;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		assert !getTableGroupJoins().contains( join );
		addJoinToSubquery( join );
		super.addTableGroupJoin( join );
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		assert !getTableGroupJoins().contains( join );
		addJoinToSubquery( join );
		super.addNestedTableGroupJoin( join );
	}

	void addJoinToSubquery(TableGroupJoin join) {
		if ( join.getJoinedGroup() instanceof StandardVirtualTableGroup ) {
			// An embedded path shares its parent's table. Its own joins reach this
			// correlated group individually, including through nested embeddables.
			registerPredicate( join );
		}
		else if ( join.getJoinType() == SqlAstJoinType.LEFT ) {
			// A correlated root has no FROM item in the subquery. Give its left joins a
			// single row to preserve when the ON predicate finds no matching target.
			if ( singleRowGroup == null ) {
				final var factory = getSessionFactory();
				final var singleRowSupport = factory.getJdbcServices().getDialect().getSingleRowTableSupport();
				final var singleRow = new NamedTableReference( "(select 1 c" + singleRowSupport.getSelectOnlyFromClause() + ")",
						getSqlAliasBase().generateNewAlias() );
				singleRowGroup = new StandardTableGroup( true, getNavigablePath(), null, null, singleRow, null, factory );
				querySpec.getFromClause().addRoot( singleRowGroup );
			}
			singleRowGroup.addTableGroupJoin( join );
		}
		else {
			assert join.getJoinType() == SqlAstJoinType.INNER;
			querySpec.getFromClause().addRoot( join.getJoinedGroup() );
			registerPredicate( join );
		}
	}

	private void registerPredicate(TableGroupJoin join) {
		if ( join.getPredicate() != null ) {
			joinPredicateConsumer.accept( join.getPredicate() );
		}
		else if ( join.getJoinedGroup() instanceof LazyTableGroup lazyTableGroup ) {
			// Wait for the table group to get initialized before consuming the predicate
			lazyTableGroup.setTableGroupInitializerCallback(
					tableGroup -> joinPredicateConsumer.accept( join.getPredicate() )
			);
		}
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression,
			boolean resolve) {
		final TableReference tableReference = correlatedTableGroup.getTableReference(
				navigablePath,
				modelPart,
				tableExpression,
				resolve
		);
		if ( tableReference != null ) {
			return tableReference;
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, modelPart, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, modelPart, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		return null;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		final TableReference tableReference = correlatedTableGroup.getTableReference(
				navigablePath,
				tableExpression,
				resolve
		);
		if ( tableReference != null ) {
			return tableReference;
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		return null;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		getPrimaryTableReference().applyAffectedTableNames( nameCollector );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return correlatedTableGroup.getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	public Consumer<Predicate> getJoinPredicateConsumer() {
		return joinPredicateConsumer;
	}

	@Override
	public TableGroup findCompatibleJoinedGroup(TableGroupJoinProducer joinProducer, SqlAstJoinType requestedJoinType) {
		final TableGroup compatibleJoinedGroup = super.findCompatibleJoinedGroup( joinProducer, requestedJoinType );
		return compatibleJoinedGroup == null ? correlatedTableGroup.findCompatibleJoinedGroup(
				joinProducer,
				requestedJoinType
		) : compatibleJoinedGroup;
	}
}
