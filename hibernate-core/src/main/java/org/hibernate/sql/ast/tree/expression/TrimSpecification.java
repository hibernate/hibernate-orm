/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.TrimSpec;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class TrimSpecification implements SqlExpressable, SqlAstNode {
	private TrimSpec trimSpec;

	public TrimSpecification(TrimSpec trimSpec) {
		this.trimSpec = trimSpec;
	}

	public TrimSpec getSpecification() {
		return trimSpec;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTrimSpecification(this);
	}
}
