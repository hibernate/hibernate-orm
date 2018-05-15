/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class CurrentTimestampFunction extends AbstractStandardFunction {
	private final SqlExpressableType type;

	public CurrentTimestampFunction(SqlExpressableType type) {
		this.type = type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCurrentTimestampFunction( this );
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}
}
