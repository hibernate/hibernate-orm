/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.TableInsert;

/**
 * Identity column support for H2 2+ versions
 * @author Jan Schatteman
 */
public class H2FinalTableIdentityColumnSupport extends H2IdentityColumnSupport {

	public static final H2FinalTableIdentityColumnSupport INSTANCE = new H2FinalTableIdentityColumnSupport();

	private H2FinalTableIdentityColumnSupport() {
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return "select " + identityColumnName + " from final table ( " + insertString + " )";
	}

	@Deprecated //TODO: move this logic back to H2SqlAstTranslator
	public void render(
			TableInsert tableInsert,
			Consumer<String> sqlAppender,
			Consumer<ColumnReference> returnColumnHandler,
			InsertValuesHandler insertValuesHandler,
			SessionFactoryImplementor sessionFactory) {
		// select col(, col)* from final table ( <real insert> )

		sqlAppender.accept( "select " );

		final List<ColumnReference> returningColumns = tableInsert.getReturningColumns();
		for ( int i = 0; i < returningColumns.size(); i++ ) {
			if ( i > 0 ) {
				sqlAppender.accept( ", " );
			}

			returnColumnHandler.accept( returningColumns.get( i ) );
		}

		sqlAppender.accept( " from final table ( " );
		insertValuesHandler.renderInsertValues();
		sqlAppender.accept( ")" );
	}
}
