/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.util.collections.UniqueList;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class SelectClause implements SqlAstNode {
	private boolean distinct;
	private final UniqueList<SqlSelection> sqlSelections = new UniqueList<>();

	public SelectClause() {
	}

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public List<SqlSelection> getSqlSelectionList() {
		return Collections.unmodifiableList( sqlSelections );
	}

	public Set<SqlSelection> getSqlSelections() {
		return Collections.unmodifiableSet( sqlSelections );
	}

	public void addSqlSelection(SqlSelection sqlSelection) {
		sqlSelections.add( sqlSelection );
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitSelectClause( this );
	}
}
