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
 * @author Christian Beikov
 */
public class Collation implements SqlExpressible, SqlAstNode {

	private final String collation;

	public Collation(String collation) {
		this.collation = collation;
	}

	public String getCollation() {
		return collation;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCollation( this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return 0;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return null;
	}
}
