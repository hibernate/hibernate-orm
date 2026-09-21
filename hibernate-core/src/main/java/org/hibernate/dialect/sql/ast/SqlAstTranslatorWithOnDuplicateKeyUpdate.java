/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;


import org.hibernate.StaleStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
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
public abstract class SqlAstTranslatorWithOnDuplicateKeyUpdate<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public SqlAstTranslatorWithOnDuplicateKeyUpdate(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	/**
	 * Create the MutationOperation for performing the DELETE or UPSERT
	 */
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

		return new DeleteOrUpsertOperation( optionalTableUpdate.getMutationTarget(),
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

	protected void renderUpsertStatement(OptionalTableUpdate optionalTableUpdate) {
		renderInsertInto( optionalTableUpdate );
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

		boolean anyUpdatable = false;
		for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
			if ( valueBinding.isAttributeInsertable() ) {
				appendSql( ',' );
				appendSql( valueBinding.getColumnReference().getColumnExpression() );
			}
			anyUpdatable |= valueBinding.isAttributeUpdatable();
		}

		appendSql( ") values " );

		separator = '(';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			keyBinding.getValueExpression().accept( this );
			separator = ',';
		}

		for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
			if ( valueBinding.isAttributeInsertable() ) {
				appendSql( ',' );
				valueBinding.getValueExpression().accept( this );
			}
		}

		appendSql(")");
		if ( anyUpdatable ) {
			renderNewRowAlias();
		}
	}

	protected void renderNewRowAlias() {
	}

	protected void renderOnDuplicateKeyUpdate(OptionalTableUpdate optionalTableUpdate) {
		appendSql( " on duplicate key update" );
		if ( optionalTableUpdate.getValueBindings().stream()
					.anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			char separator = ' ';
			for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
				if ( valueBinding.isAttributeUpdatable() ) {
					appendSql( separator );
					appendSql( valueBinding.getColumnReference().getColumnExpression() );
					append( '=' );
					renderUpdateValue( valueBinding );
					separator = ',';
				}
			}
		}
		else {
			final String keyColName =
					optionalTableUpdate.getKeyBindings().get( 0 )
							.getColumnReference().getColumnExpression();
			appendSql( ' ' );
			appendSql( keyColName );
			appendSql( '=' );
			appendSql( keyColName );
		}
	}

	protected abstract void renderUpdateValue(ColumnValueBinding columnValueBinding);

}
