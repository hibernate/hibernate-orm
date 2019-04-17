package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class CastTarget implements SqlExpressable, SqlAstNode {
	private SqlExpressableType type;

	public CastTarget(SqlExpressableType type) {
		this.type = type;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastTarget(this);
	}
}
