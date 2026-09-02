/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/// Provider factory which reuses Hibernate's standard factory template while
/// supplying the provider's translator for query and model-mutation paths.
///
/// @author Steve Ebersole
public final class ExampleSqlAstTranslatorFactory extends StandardSqlAstTranslatorFactory {
	public static final ExampleSqlAstTranslatorFactory INSTANCE = new ExampleSqlAstTranslatorFactory();

	private ExampleSqlAstTranslatorFactory() {
	}

	@Override
	protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
		return new ExampleSqlAstTranslator<>( request );
	}
}
