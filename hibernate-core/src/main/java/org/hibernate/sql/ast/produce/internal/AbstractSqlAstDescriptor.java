/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.Set;

import org.hibernate.sql.ast.produce.spi.SqlAstDescriptor;
import org.hibernate.sql.ast.tree.Statement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstDescriptor implements SqlAstDescriptor {
	private final Statement sqlAstStatement;
	private final Set<String> affectedTables;

	public AbstractSqlAstDescriptor(Statement sqlAstStatement, Set<String> affectedTables) {
		this.sqlAstStatement = sqlAstStatement;
		this.affectedTables = affectedTables;
	}

	@Override
	public Statement getSqlAstStatement() {
		return sqlAstStatement;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTables;
	}
}
