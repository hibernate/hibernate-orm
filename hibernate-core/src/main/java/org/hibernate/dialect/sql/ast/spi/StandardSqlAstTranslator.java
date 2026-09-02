/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;

/**
 * The final phase of query translation. Here we take the SQL AST an
 * "interpretation". For a select query, that means an instance of
 * {@link JdbcOperationQuerySelect}.
 *
 * @author Christian Beikov
 */
public class StandardSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public StandardSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}
}
