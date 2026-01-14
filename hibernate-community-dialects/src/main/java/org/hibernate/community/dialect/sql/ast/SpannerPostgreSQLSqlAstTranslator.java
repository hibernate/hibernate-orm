/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sql.ast;

import org.hibernate.dialect.sql.ast.PostgreSQLSqlAstTranslator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.exec.spi.JdbcOperation;

public class SpannerPostgreSQLSqlAstTranslator<T extends JdbcOperation>
		extends PostgreSQLSqlAstTranslator<T> {

	public SpannerPostgreSQLSqlAstTranslator(
			SessionFactoryImplementor sessionFactory, Statement statement) {
		super(sessionFactory, statement);
	}

	@Override
	protected void renderMaterializationHint(CteMaterialization materialization) {
		throw new UnsupportedOperationException("Spanner does not support CTE materialization");
	}

	@Override
	protected String defaultEscapeCharacter() {
		return "'\\'";
	}
}
