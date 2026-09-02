/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

/**
 * Represents the format pattern for a date/time format expression
 *
 * @author Gavin King
 */
public class Format implements SqlExpressible, SqlAstNode {
	private final String format;

	public Format(String format) {
		this.format = format;
	}

	public String getFormat() {
		return format;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitFormat( this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getJdbcTypeCount();
	}
}
