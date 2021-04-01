/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public class StandardTableGroup extends AbstractTableGroup {
	private final TableReference primaryTableReference;
	private final Predicate<String> tableReferenceJoinNameChecker;
	private final BiFunction<String,TableGroup,TableReferenceJoin> tableReferenceJoinCreator;
	private final boolean realTableGroup;

	private List<TableReferenceJoin> tableJoins;

	public StandardTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			LockMode lockMode,
			TableReference primaryTableReference,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		super( navigablePath, tableGroupProducer, lockMode, sqlAliasBase, sessionFactory );
		this.primaryTableReference = primaryTableReference;
		this.realTableGroup = false;
		this.tableJoins = Collections.emptyList();
		this.tableReferenceJoinCreator = null;
		this.tableReferenceJoinNameChecker = s -> {
			for ( int i = 0; i < tableJoins.size(); i++ ) {
				if ( tableJoins.get( i ).getJoinedTableReference().getTableExpression().equals( s ) ) {
					return true;
				}
			}
			return false;
		};
	}

	public StandardTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			LockMode lockMode,
			TableReference primaryTableReference,
			boolean realTableGroup,
			SqlAliasBase sqlAliasBase,
			Predicate<String> tableReferenceJoinNameChecker,
			BiFunction<String, TableGroup, TableReferenceJoin> tableReferenceJoinCreator,
			SessionFactoryImplementor sessionFactory) {
		super( navigablePath, tableGroupProducer, lockMode, sqlAliasBase, sessionFactory );
		this.primaryTableReference = primaryTableReference;
		this.realTableGroup = realTableGroup;
		this.tableJoins = null;
		this.tableReferenceJoinNameChecker = tableReferenceJoinNameChecker;
		this.tableReferenceJoinCreator = tableReferenceJoinCreator;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		// todo (6.0) : if we implement dynamic TableReference creation, this still needs to return the expressions for all mapped tables not just the ones with a TableReference at this time
		nameCollector.accept( getPrimaryTableReference().getTableExpression() );
		for ( TableReferenceJoin tableReferenceJoin : tableJoins ) {
			nameCollector.accept( tableReferenceJoin.getJoinedTableReference().getTableExpression() );
		}
	}

	@Override
	public TableReference getTableReference(String tableExpression) {
		return getTableReferenceInternal( tableExpression );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableJoins == null ? Collections.emptyList() : tableJoins;
	}

	@Override
	public boolean isRealTableGroup() {
		return realTableGroup;
	}

	public void addTableReferenceJoin(TableReferenceJoin join) {
		if ( tableJoins == null ) {
			tableJoins = new ArrayList<>();
		}
		tableJoins.add( join );
	}

	@Override
	public TableReference getTableReferenceInternal(String tableExpression) {
		TableReference tableReference = primaryTableReference.getTableReference( tableExpression );
		if ( tableReference != null ) {
			return tableReference;
		}

		if ( tableReferenceJoinNameChecker.test( tableExpression ) ) {
			if ( tableJoins != null ) {
				for ( int i = 0; i < tableJoins.size(); i++ ) {
					final TableReferenceJoin join = tableJoins.get( i );
					assert join != null;
					final TableReference resolveTableReference = join.getJoinedTableReference()
							.getTableReference( tableExpression );
					if ( resolveTableReference != null ) {
						return resolveTableReference;
					}
				}
			}

			return potentiallyCreateTableReference( tableExpression );
		}

		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference primaryTableReference = tableGroupJoin.getJoinedGroup().getPrimaryTableReference();
			if ( primaryTableReference.getTableReference( tableExpression ) != null ) {
				return primaryTableReference;
			}
		}

		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected TableReference potentiallyCreateTableReference(String tableExpression) {
		final TableReferenceJoin join = tableReferenceJoinCreator.apply( tableExpression, this );
		if ( join != null ) {
			addTableReferenceJoin( join );
			return join.getJoinedTableReference();
		}
		return null;
	}
}
