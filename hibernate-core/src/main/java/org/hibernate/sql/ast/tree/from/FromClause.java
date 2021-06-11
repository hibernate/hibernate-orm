/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * The SQL AST from-clause node
 *
 * @author Steve Ebersole
 */
public class FromClause implements SqlAstNode {
	private final List<TableGroup> roots;

	public FromClause() {
		roots = new ArrayList<>();
	}

	public FromClause(int expectedNumberOfRoots) {
		roots = CollectionHelper.arrayList( expectedNumberOfRoots );
	}

	public List<TableGroup> getRoots() {
		return roots;
	}

	public void addRoot(TableGroup tableGroup) {
		roots.add( tableGroup );
	}

	public void visitRoots(Consumer<TableGroup> action) {
		roots.forEach( action );
	}

	public void visitTableGroups(Consumer<TableGroup> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			visitTableGroups( roots.get( i ), action );
		}
	}

	private void visitTableGroups(TableGroup tableGroup, Consumer<TableGroup> action) {
		action.accept( tableGroup );
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			visitTableGroups( tableGroupJoins.get( i ).getJoinedGroup(), action );
		}
	}

	public <T> T queryTableGroups(Function<TableGroup, T> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			final T result = queryTableGroups( roots.get( i ), action );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private <T> T queryTableGroups(TableGroup tableGroup, Function<TableGroup, T> action) {
		final T result = action.apply( tableGroup );
		if ( result != null ) {
			return result;
		}
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final T nestedResult = queryTableGroups( tableGroupJoins.get( i ).getJoinedGroup(), action );
			if ( nestedResult != null ) {
				return nestedResult;
			}
		}
		return null;
	}

	public void visitTableJoins(Consumer<TableJoin> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			visitTableJoins( roots.get( i ), action );
		}
	}

	private void visitTableJoins(TableGroup tableGroup, Consumer<TableJoin> action) {
		tableGroup.getTableReferenceJoins().forEach( action );
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
			action.accept( tableGroupJoin );
			visitTableJoins( tableGroupJoin.getJoinedGroup(), action );
		}
	}

	public <T> T queryTableJoins(Function<TableJoin, T> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			final T result = queryTableJoins( roots.get( i ), action );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private <T> T queryTableJoins(TableGroup tableGroup, Function<TableJoin, T> action) {
		for ( TableReferenceJoin tableReferenceJoin : tableGroup.getTableReferenceJoins() ) {
			final T result = action.apply( tableReferenceJoin );
			if ( result != null ) {
				return result;
			}
		}

		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
			T result = action.apply( tableGroupJoin );
			if ( result != null ) {
				return result;
			}
			result = queryTableJoins( tableGroupJoin.getJoinedGroup(), action );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	public void visitTableGroupJoins(Consumer<TableGroupJoin> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			visitTableGroupJoins( roots.get( i ), action );
		}
	}

	private void visitTableGroupJoins(TableGroup tableGroup, Consumer<TableGroupJoin> action) {
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
			action.accept( tableGroupJoin );
			visitTableGroupJoins( tableGroupJoin.getJoinedGroup(), action );
		}
	}

	public <T> T queryTableGroupJoins(Function<TableGroupJoin, T> action) {
		for ( int i = 0; i < roots.size(); i++ ) {
			final T result = queryTableGroupJoins( roots.get( i ), action );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private <T> T queryTableGroupJoins(TableGroup tableGroup, Function<TableGroupJoin, T> action) {
		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		for ( int i = 0; i < tableGroupJoins.size(); i++ ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoins.get( i );
			T result = action.apply( tableGroupJoin );
			if ( result != null ) {
				return result;
			}
			result = queryTableGroupJoins( tableGroupJoin.getJoinedGroup(), action );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFromClause( this );
	}
}
