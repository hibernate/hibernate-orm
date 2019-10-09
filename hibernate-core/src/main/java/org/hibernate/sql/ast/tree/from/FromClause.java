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

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.spi.SqlAstWalker;
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

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFromClause( this );
	}
}
