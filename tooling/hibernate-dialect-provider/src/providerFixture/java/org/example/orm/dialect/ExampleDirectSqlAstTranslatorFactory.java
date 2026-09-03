/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;

/// Example factory which creates direct, non-SQL translator implementations.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ExampleDirectSqlAstTranslatorFactory implements SqlAstTranslatorFactory {
	@Override
	public <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> buildTranslator(
			SqlAstTranslationRequest<S, O> request) {
		return new ExampleDirectSqlAstTranslator<>( request );
	}
}
