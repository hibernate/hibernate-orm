/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.util.List;

import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.spi.Return;

/**
 * Represents the {@link SqlSelectAstToJdbcSelectConverter}'s interpretation of a select query
 *
 * @author Steve Ebersole
 */
public interface JdbcSelect extends JdbcOperation {
	List<SqlSelection> getSqlSelections();
	List<Return> getReturns();
}
