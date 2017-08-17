/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class CurrentDateFunction extends AbstractStandardFunction {
	private final BasicValuedExpressableType type;

	public CurrentDateFunction(BasicValuedExpressableType type) {
		this.type = type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCurrentDateFunction( this );
	}

	@Override
	public BasicValuedExpressableType getType() {
		return type;
	}
}
