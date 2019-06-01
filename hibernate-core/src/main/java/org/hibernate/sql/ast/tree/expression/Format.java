/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class Format implements SqlExpressable, SqlAstNode {
	private String format;
	private SqlExpressableType type;

	public Format(String format, SqlExpressableType type) {
		this.format = format;
		this.type = type;
	}

	public String getFormat() {
		return format;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitFormat(this);
	}
}
