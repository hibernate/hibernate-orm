/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.SqlExpressableType;

/**
 * Unifying contract for things that are capable of being an expression in
 * the SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqlExpressable {
	/**
	 * Any thing that is expressable at the SQL AST level
	 * would be of basic type.
	 */
	SqlExpressableType getExpressableType();
}
