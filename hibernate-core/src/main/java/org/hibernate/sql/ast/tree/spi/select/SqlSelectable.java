/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * Unifying contract for things that are selectable at the SQL level.
 *
 * todo (6.0) : org.hibernate.persister.common.spi.Column extends this?
 * 		if so, nice way to handle physical versus formula in terms of walking.
 *
 * @author Steve Ebersole
 */
public interface SqlSelectable {
	SqlSelectionReader getSqlSelectionReader();

	void accept(SqlSelectAstToJdbcSelectConverter interpreter);
}
