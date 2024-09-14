/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.Statement;

/**
 * @author Steve Ebersole
 */
public interface SqmTranslator<T extends Statement>
		extends SqmToSqlAstConverter, FromClauseAccess, JdbcParameterBySqmParameterAccess {
	SqmTranslation<T> translate();
}
