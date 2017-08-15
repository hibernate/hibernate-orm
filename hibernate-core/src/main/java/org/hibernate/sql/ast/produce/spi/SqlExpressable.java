/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionReader;

/**
 * Unifying contract for things that are capable of being an expression at
 * the SQL level.
 *
 * Such an expressable can also be part of the SQL select-clause
 *
 * @author Steve Ebersole
 */
public interface SqlExpressable {
	// todo (6.0) : another option here is to have this expose `#generateSqlSelection`
	//		`SqlSelection` has already been changed to expose the SqlSelectionReader.
	//		so the idea here is that when the SqlExpressable is used in the
	// 		(root) select-clause we'd ask it to build the SqlSelection which
	//		in turn can give us the SqlSelectionReader.  This makes sense because
	//		the expressable already knows the reader.  From a design perspective
	//		it also makes sense in terms of being a natural association and flow.
	//
	/**
	 * If this expressable is used as a selection in the SQL, return the
	 * reader that can be used to read its value.
	 */
	SqlSelectionReader getSqlSelectionReader();

	/**
	 * If this expressable is used as a selection in the SQL, this method
	 * will be called to generate the corresponding return the
	 * reader that can be used to read its value.
	 */
	SqlSelection generateSqlSelection(int jdbcResultSetPosition);
}
