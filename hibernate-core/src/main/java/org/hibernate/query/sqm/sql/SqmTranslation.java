/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Information obtained from the interpretation of an SqmStatement
 *
 * @author Steve Ebersole
 */
public interface SqmTranslation {
	Statement getSqlAst();
	Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam();
}
