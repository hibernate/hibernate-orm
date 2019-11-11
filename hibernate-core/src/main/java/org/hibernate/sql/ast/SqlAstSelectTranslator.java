/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.spi.SqlAstToJdbcOperationConverter;
import org.hibernate.sql.ast.spi.SqlSelectAstWalker;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public interface SqlAstSelectTranslator extends SqlSelectAstWalker, SqlAstToJdbcOperationConverter {

	/**
	 * Translate the SelectStatement into the executable JdbcSelect
	 */
	JdbcSelect translate(SelectStatement selectStatement);

	/**
	 * Translate the QuerySpec into the executable JdbcSelect
	 */
	JdbcSelect translate(QuerySpec querySpec);
}
