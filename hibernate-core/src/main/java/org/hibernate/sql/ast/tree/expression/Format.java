/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlExpressable;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Represents the format pattern for a date/time format expression
 *
 * @author Gavin King
 */
public class Format implements SqlExpressable, SqlAstNode {
	private String format;
	private BasicValuedMapping type;

	public Format(String format, BasicValuedMapping type) {
		this.format = format;
		this.type = type;
	}

	public String getFormat() {
		return format;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type.getJdbcMapping();
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitFormat( this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, type.getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
