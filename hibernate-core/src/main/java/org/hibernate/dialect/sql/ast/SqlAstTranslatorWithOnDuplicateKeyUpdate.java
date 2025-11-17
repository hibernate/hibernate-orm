/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;


import org.hibernate.dialect.MySQLDeleteOrUpsertOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.ast.spi.SqlAstTranslatorWithUpsert;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.UpsertOperation;

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
				getParameterBinders()
		);

		return new MySQLDeleteOrUpsertOperation(
				optionalTableUpdate.getMutationTarget(),
				(EntityTableMapping) optionalTableUpdate.getMutatingTable().getTableMapping(),
				upsertOperation,
				optionalTableUpdate
		);
	}

	@Override
	protected void renderUpsertStatement(OptionalTableUpdate optionalTableUpdate) {
		renderInsertInto( optionalTableUpdate );
		appendSql( " " );
		renderOnDuplicateKeyUpdate( optionalTableUpdate );
	}

	protected void renderInsertInto(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "insert into " );
		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		appendSql( " (" );

		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( keyBinding.getColumnReference().getColumnExpression() );
			appendSql( ',' );
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
				appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
			if ( columnPosition != optionalTableUpdate.getValueBindings().size() - 1  ) {
				appendSql( ',' );
			}
		} );

		appendSql( ") values (" );

		for ( ColumnValueBinding keyBinding : keyBindings ) {
			keyBinding.getValueExpression().accept( this );
			appendSql( ',' );
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			if ( columnPosition > 0 ) {
				appendSql( ',' );
			}
			columnValueBinding.getValueExpression().accept( this );
		} );
		appendSql(") ");
		renderNewRowAlias();
	}

	protected void renderNewRowAlias() {
	}

	protected void renderOnDuplicateKeyUpdate(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "on duplicate key update " );
		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			final String columnName = columnValueBinding.getColumnReference().getColumnExpression();
			if ( columnPosition > 0 ) {
				appendSql( ',' );
			}
			appendSql( columnName );
			append( " = " );
			renderUpdatevalue( columnValueBinding );
		} );
	}

	protected void renderUpdatevalue(ColumnValueBinding columnValueBinding) {
	}

}
