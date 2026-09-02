/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


import org.hibernate.SPI;
import org.hibernate.StaleStateException;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.spi.mutation.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.spi.mutation.jdbc.UpsertOperation;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author Jan Schatteman
 */
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class SqlAstTranslatorWithOnDuplicateKeyUpdate<T extends JdbcOperation> extends SqlAstTranslatorWithUpsert<T> {

	@SPI(SPI.Role.IMPLEMENT)
	public SqlAstTranslatorWithOnDuplicateKeyUpdate(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
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
			if ( columnValueBinding.isAttributeInsertable() ) {
				appendSql( ',' );
				columnValueBinding.getValueExpression().accept( this );
			}
		} );
		appendSql(") ");
		if ( optionalTableUpdate.getValueBindings().stream()
				.anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			renderNewRowAlias();
		}
	}

	protected void renderNewRowAlias() {
	}

	protected void renderOnDuplicateKeyUpdate(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "on duplicate key update " );
		if ( optionalTableUpdate.getValueBindings().stream()
					.anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			class BindingProcessor implements BiConsumer<Integer, ColumnValueBinding> {
				boolean first = true;
				@Override
				public void accept(Integer columnPosition, ColumnValueBinding columnValueBinding) {
					if ( columnValueBinding.isAttributeUpdatable() ) {
						final String columnName = columnValueBinding.getColumnReference().getColumnExpression();
						if ( first ) {
							first = false;
						}
						else {
							appendSql( ',' );
						}
						appendSql( columnName );
						append( " = " );
						renderUpdateValue( columnValueBinding );
					}
				}
			}
			optionalTableUpdate.forEachValueBinding( new BindingProcessor() );
		}
		else {
			final String keyColName =
					optionalTableUpdate.getKeyBindings().get( 0 )
							.getColumnReference().getColumnExpression();
			appendSql( keyColName );
			appendSql( "=" );
			appendSql( keyColName );
		}
	}

	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
	}

}
