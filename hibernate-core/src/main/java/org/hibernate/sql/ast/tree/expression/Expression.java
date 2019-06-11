/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.persister.SqlExpressableType;
import org.hibernate.sql.ast.spi.SqlSelectionProducer;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Models an expression at the SQL-level.
 *
 * @author Steve Ebersole
 */
public interface Expression extends SqlAstNode, SqlSelectionProducer {
	//
	/**
	 * Access the type for this expression.
	 */
	SqlExpressableType getType();
}
