/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;

/**
 * The final phase of query translation. Here we take the SQL AST an
 * "interpretation". For a select query, that means an instance of
 * {@link JdbcOperationQuerySelect}.
 *
 * @author Christian Beikov
 */
public class StandardSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	@Deprecated(forRemoval = true, since = "7.1")
	public StandardSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	public StandardSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
	}
}
