/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.results.spi.Selectable;

/**
 * @author Steve Ebersole
 */
public interface GenericParameter
		extends SqlAstNode, SqlExpressable, Expression, Selectable {
	JdbcParameterBinder getParameterBinder();

	@Override
	default SqlExpressable getExpressable() {
		return this;
	}
}
