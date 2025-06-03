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
/*
		Template: (for an entity with @Version, and without using values() - but this might require changes in parameter binding)
			INSERT INTO employees (id, name, salary, version)
				VALUES (?, ?, ?, ?) AS t
			ON DUPLICATE KEY UPDATE
				name = IF(employees.version=?,t.name,employees.name),
				salary = IF(employees.version=?,t.salary,employees.salary),
				version = IF(employees.version=?,t.version,employees.version),

		So, initially we'll have:
			INSERT INTO employees (id, name, salary, version)
				VALUES (?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				name = IF(version=@oldversion:=?,VALUES(name), employees.name),
				salary = IF(version=@oldversion?,VALUES(salary),employees.salary),
				version = IF(version=@oldversion?,VALUES(version),employees.version),
*/
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
		appendSql( ")" );
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

			if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() > 0 ) {
				renderVersionedUpdate( optionalTableUpdate, columnPosition, columnValueBinding );
			}
			else {
				renderNonVersionedUpdate( columnValueBinding );
			}
		} );
	}

	private void renderVersionedUpdate(OptionalTableUpdate optionalTableUpdate, Integer columnPosition, ColumnValueBinding columnValueBinding) {
		final String tableName = optionalTableUpdate.getMutatingTable().getTableName();
		appendSql( "if(" );
		renderVersionRestriction( tableName, optionalTableUpdate.getOptimisticLockBindings(), columnPosition );
		appendSql( "," );
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
		appendSql( "," );
		columnValueBinding.getColumnReference().appendColumnForWrite( this, tableName );
		appendSql( ")" );
	}

	private void renderNonVersionedUpdate(ColumnValueBinding columnValueBinding) {
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
	}

	private void renderVersionRestriction(String tableName, List<ColumnValueBinding> optimisticLockBindings, int index) {
		final String operator = index == 0 ? ":=" : "";
		final String versionVariable = "@oldversion" + operator;
		for (int i = 0; i < optimisticLockBindings.size(); i++) {
//			if ( i>0 ) {
//				appendSql(" and ");
//			}
			final ColumnValueBinding binding = optimisticLockBindings.get( i );
			binding.getColumnReference().appendColumnForWrite( this, tableName );
			appendSql( "=" );
			appendSql( versionVariable );
//			if ( i == 0 ) {
				if ( index == 0) {
					binding.getValueExpression().accept( this );
				}
//			}
		}
	}

}
