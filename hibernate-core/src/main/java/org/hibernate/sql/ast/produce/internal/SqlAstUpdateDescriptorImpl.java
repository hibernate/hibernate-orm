/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.Set;

import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;

/**
 * The standard Hibernate implementation of SqlAstUpdateDescriptor
 *
 * @author Steve Ebersole
 */
public class SqlAstUpdateDescriptorImpl implements SqlAstUpdateDescriptor {
	private final UpdateStatement sqlAst;
	private final Set<String> affectedTableNames;

	public SqlAstUpdateDescriptorImpl(UpdateStatement sqlAst, Set<String> affectedTableNames) {
		this.sqlAst = sqlAst;
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public UpdateStatement getSqlAstStatement() {
		return sqlAst;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
