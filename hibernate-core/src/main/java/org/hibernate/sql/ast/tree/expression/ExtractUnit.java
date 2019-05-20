/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.query.TemporalUnit;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class ExtractUnit implements SqlExpressable, SqlAstNode {
	private TemporalUnit unit;
	private SqlExpressableType type;

	public ExtractUnit(TemporalUnit unit, SqlExpressableType type) {
		this.unit = unit;
		this.type = type;
	}

	public TemporalUnit getUnit() {
		return unit;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitExtractUnit(this);
	}
}

