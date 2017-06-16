/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;

/**
 * @author Steve Ebersole
 */
public interface InFlightJdbcOperation extends JdbcCall {

	void setFunctionReturn(JdbcCallFunctionReturn functionReturn);

	void addParameterRegistration(JdbcCallParameterRegistration registration);

	void addSqlSelection(SqlSelection sqlSelection);

	void addQueryReturn(QueryResult queryReturn);
}
