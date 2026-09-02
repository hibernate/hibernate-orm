/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/// External provider translator which customizes full-MERGE grammar through a
/// supported rendering callback.
///
/// @param <T> the translated JDBC operation type
///
/// @since 8.0
/// @author Steve Ebersole
// tag::full-merge-translator[]
public final class ExampleMergeSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {
	/// Creates a fixture translator for one translation request.
	///
	/// @since 8.0
	public ExampleMergeSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	/// Renders the fixture's target alias without an `as` keyword.
	///
	/// @since 8.0
	@Override
	protected void renderMergeTargetAlias() {
		appendSql( "fixture_target" );
	}
}
// end::full-merge-translator[]
