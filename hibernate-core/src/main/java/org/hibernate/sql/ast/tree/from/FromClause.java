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
import java.util.function.Consumer;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class FromClause implements SqlAstNode {
	private final List<TableGroup> roots = new ArrayList<>();

	public FromClause() {
	}

	public List<TableGroup> getRoots() {
		return Collections.unmodifiableList( roots );
	}

	public void addRoot(TableGroup root) {
		this.roots.add( root );
	}

	public void visitRoots(Consumer<TableGroup> consumer) {
		roots.forEach( consumer );
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitFromClause( this );
	}
}
