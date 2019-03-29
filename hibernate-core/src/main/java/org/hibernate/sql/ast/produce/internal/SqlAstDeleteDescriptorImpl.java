/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.Set;

import org.hibernate.sql.ast.produce.spi.SqlAstDeleteDescriptor;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;

/**
 * @author Steve Ebersole
 */
public class SqlAstDeleteDescriptorImpl extends AbstractSqlAstDescriptor implements SqlAstDeleteDescriptor {
	public SqlAstDeleteDescriptorImpl(
			DeleteStatement sqlAstStatement,
			Set<String> affectedTables) {
		super( sqlAstStatement, affectedTables );
	}

	@Override
	public DeleteStatement getSqlAstStatement() {
		return (DeleteStatement) super.getSqlAstStatement();
	}
}
