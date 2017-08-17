/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class FromClause implements SqlAstNode {
	private final List<TableSpace> tableSpaces = new ArrayList<>();

	public FromClause() {
	}

	public List<TableSpace> getTableSpaces() {
		return Collections.unmodifiableList( tableSpaces );
	}

	public TableSpace makeTableSpace() {
		final TableSpace tableSpace = new TableSpace( this );
		addTableSpace( tableSpace );
		return tableSpace;
	}

	public void addTableSpace(TableSpace tableSpace) {
		tableSpaces.add( tableSpace );
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitFromClause( this );
	}
}
