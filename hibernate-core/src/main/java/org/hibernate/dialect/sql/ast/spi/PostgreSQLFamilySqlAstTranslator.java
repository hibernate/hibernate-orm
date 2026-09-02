/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported SQL AST translator base for PostgreSQL-derived databases.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class PostgreSQLFamilySqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {
	@SPI(IMPLEMENT)
	protected PostgreSQLFamilySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@SPI(IMPLEMENT)
	protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
		if ( context == TableReferenceAliasContext.INSERT_TARGET ) {
			appendSql( " as " );
			appendSql( alias );
		}
		else {
			super.renderTableReferenceAlias( alias, context );
		}
	}
}
