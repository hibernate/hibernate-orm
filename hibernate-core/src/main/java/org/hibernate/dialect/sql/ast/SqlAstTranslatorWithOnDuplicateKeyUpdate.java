/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;


import org.hibernate.StaleStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.ast.spi.SqlAstTranslatorWithUpsert;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.model.jdbc.UpsertOperation;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * @author Jan Schatteman
 */
public class SqlAstTranslatorWithOnDuplicateKeyUpdate<T extends JdbcOperation> extends SqlAstTranslatorWithUpsert<T> {

	public SqlAstTranslatorWithOnDuplicateKeyUpdate(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public MutationOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		assert optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0;

		renderUpsertStatement( optionalTableUpdate );

		final UpsertOperation upsertOperation = new UpsertOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				new MySQLRowCountExpectation(),
				getParameterBinders()
		);

		return new DeleteOrUpsertOperation(
				optionalTableUpdate.getMutationTarget(),
				(EntityTableMapping) optionalTableUpdate.getMutatingTable().getTableMapping(),
				upsertOperation,
				optionalTableUpdate
		);
	}

	private static class MySQLRowCountExpectation implements Expectation {
		@Override
		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			if ( rowCount > 2 ) {
				throw new StaleStateException(
						"Unexpected row count"
						+ " (the expected row count for an ON DUPLICATE KEY UPDATE statement should be either 0, 1 or 2 )"
						+ " [" + sql + "]"
				);
			}
		}
	}

	@Override
	protected void renderUpsertStatement(OptionalTableUpdate optionalTableUpdate) {
		renderInsertInto( optionalTableUpdate );
		appendSql( " " );
		renderOnDuplicateKeyUpdate( optionalTableUpdate );
	}

	protected void renderInsertInto(OptionalTableUpdate optionalTableUpdate) {
		if ( optionalTableUpdate.getValueBindings().isEmpty() ) {
			appendSql( "insert ignore into " );
		}
		else {
			appendSql( "insert into " );
		}
		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		appendSql( " " );

		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();
		char separator = '(';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			appendSql( keyBinding.getColumnReference().getColumnExpression() );
			separator = ',';
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			appendSql( ',' );
			appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		} );

		appendSql( ") values " );

		separator = '(';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			keyBinding.getValueExpression().accept( this );
			separator = ',';
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			appendSql( ',' );
			columnValueBinding.getValueExpression().accept( this );
		} );
		appendSql(") ");
		renderNewRowAlias();
	}

	protected void renderNewRowAlias() {
	}

	protected void renderOnDuplicateKeyUpdate(OptionalTableUpdate optionalTableUpdate) {
		if  ( !optionalTableUpdate.getValueBindings().isEmpty() ) {
			appendSql( "on duplicate key update " );
			optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
				final String columnName = columnValueBinding.getColumnReference().getColumnExpression();
				if ( columnPosition > 0 ) {
					appendSql( ',' );
				}
				appendSql( columnName );
				append( " = " );
				renderUpdateValue( columnValueBinding );
			} );
		}
	}

	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
	}

}
