/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Gavin King
 */
public class Star implements Expression {
	/**
	 * Singleton access
	 */
	public static final Star INSTANCE = new Star();

	@Override
	public MappingModelExpressible getExpressionType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitStar( this );
	}
}
