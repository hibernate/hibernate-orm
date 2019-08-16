/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * The SELECT CLAUSE in the SQL AST.  Each selection here is a
 * {@link DomainResultProducer}
 *
 * @author Steve Ebersole
 */
public class SelectClause implements SqlAstNode {
	private boolean distinct;

	private final List<SqlSelection> sqlSelections = new ArrayList<>();

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void addSqlSelection(SqlSelection sqlSelection) {
		sqlSelections.add( sqlSelection );
	}

	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelectClause( this );
	}
}
