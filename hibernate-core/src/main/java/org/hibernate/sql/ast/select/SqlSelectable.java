/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * Unifying contract for things that are selectable at the SQL level
 *
 * @author Steve Ebersole
 */
public interface SqlSelectable {
	SqlSelectionReader getSqlSelectionReader();

	void accept(SqlSelectAstToJdbcSelectConverter interpreter);
}
