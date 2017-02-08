/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.expression;

import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface Expression {
	/**
	 * Obtain the ORM Type of this expression
	 *
	 * @return The ORM mapping Type
	 */
	Type getType();

	Selectable getSelectable();

	void accept(SqlSelectAstToJdbcSelectConverter walker);
}
