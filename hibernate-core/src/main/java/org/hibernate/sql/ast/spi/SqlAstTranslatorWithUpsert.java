/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.model.jdbc.UpsertOperation;

/**
 * Base SqlAstTranslator for translators which support an insert-or-update (UPSERT) command
 *
 * @author Steve Ebersole
 */
public class SqlAstTranslatorWithUpsert<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	protected SqlAstTranslatorWithUpsert(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	/**
	 * Create the MutationOperation for performing the DELETE or UPSERT
	 */
	public MutationOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderUpsertStatement( optionalTableUpdate );

		final UpsertOperation upsertOperation = new UpsertOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				// Without value bindings, the upsert may have an update count of 0
				optionalTableUpdate.getValueBindings().isEmpty()
						? new Expectation.OptionalRowCount()
						: new Expectation.RowCount(),
				getParameterBinders()
		);

		return new DeleteOrUpsertOperation(
				optionalTableUpdate.getMutationTarget(),
				(EntityTableMapping) optionalTableUpdate.getMutatingTable().getTableMapping(),
				upsertOperation,
				optionalTableUpdate
		);
	}

	protected void renderUpsertStatement(OptionalTableUpdate optionalTableUpdate) {
		// template:
		//
		// merge into [table] as t
		// using values([bindings]) as s ([column-names])
		// on t.[key] = s.[key]
		// when not matched
		// 		then insert ...
		// when matched
		//		then update ...

		renderMergeInto( optionalTableUpdate );
		appendSql( " " );
		renderMergeUsing( optionalTableUpdate );
		appendSql( " " );
		renderMergeOn( optionalTableUpdate );
		appendSql( " " );
		renderMergeInsert( optionalTableUpdate );
		appendSql( " " );
		renderMergeUpdate( optionalTableUpdate );
	}

	protected void renderMergeInto(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "merge into " );
		renderMergeTarget( optionalTableUpdate );
	}

	private void renderMergeTarget(OptionalTableUpdate optionalTableUpdate) {
		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		renderMergeTargetAlias();
	}

	protected void renderMergeTargetAlias() {
		appendSql( " as t" );
	}

	protected void renderMergeUsing(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "using " );

		renderMergeSource( optionalTableUpdate );
	}

	protected boolean wrapMergeSourceExpression() {
		return true;
	}

	protected void renderMergeSource(OptionalTableUpdate optionalTableUpdate) {
		if ( wrapMergeSourceExpression() ) {
			appendSql( " (" );
		}

		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();

		final StringBuilder columnList = new StringBuilder();

		appendSql( " values (" );

		for ( int i = 0; i < keyBindings.size(); i++ ) {
			final ColumnValueBinding keyBinding = keyBindings.get( i );
			if ( i > 0 ) {
				appendSql( ", " );
				columnList.append( ", " );
			}
			columnList.append( keyBinding.getColumnReference().getColumnExpression() );
			final ColumnWriteFragment valueExpression = keyBinding.getValueExpression();
			if ( valueExpression.getExpressionType().getJdbcType().isWriteExpressionTyped( getDialect() ) ) {
				valueExpression.accept( this );
			}
			else {
				renderCasted( valueExpression );
			}
		}
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			appendSql( ", " );
			columnList.append( ", " );
			final ColumnValueBinding valueBinding = valueBindings.get( i );
			columnList.append( valueBinding.getColumnReference().getColumnExpression() );
			final ColumnWriteFragment valueExpression = valueBinding.getValueExpression();
			if ( valueExpression.getExpressionType().getJdbcType().isWriteExpressionTyped( getDialect() ) ) {
				valueExpression.accept( this );
			}
			else {
				renderCasted( valueExpression );
			}
		}

		appendSql( ") " );

		if ( wrapMergeSourceExpression() ) {
			appendSql( ") " );
		}

		renderMergeSourceAlias();

		appendSql( "(" );
		appendSql( columnList.toString() );
		appendSql( ")" );
	}

	protected void renderMergeSourceAlias() {
		appendSql( " as s" );
	}

	protected void renderMergeOn(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "on (" );

		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();
		for ( int i = 0; i < keyBindings.size(); i++ ) {
			final ColumnValueBinding keyBinding = keyBindings.get( i );
			if ( i > 0 ) {
				appendSql( " and " );
			}
			keyBinding.getColumnReference().appendReadExpression( this, "t" );
			appendSql( "=" );
			keyBinding.getColumnReference().appendReadExpression( this, "s" );
		}
		// todo : optimistic locks?

		appendSql( ")" );
	}

	protected void renderMergeInsert(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();

		final StringBuilder valuesList = new StringBuilder();

		appendSql( "when not matched then insert (" );
		for ( int i = 0; i < keyBindings.size(); i++ ) {
			if ( i > 0 ) {
				appendSql( ", " );
				valuesList.append( ", " );
			}
			final ColumnValueBinding keyBinding = keyBindings.get( i );
			appendSql( keyBinding.getColumnReference().getColumnExpression() );
			keyBinding.getColumnReference().appendReadExpression( "s", valuesList::append );
		}
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			appendSql( ", " );
			valuesList.append( ", " );
			final ColumnValueBinding valueBinding = valueBindings.get( i );
			appendSql( valueBinding.getColumnReference().getColumnExpression() );
			valueBinding.getColumnReference().appendReadExpression( "s", valuesList::append );
		}

		appendSql( ") values (" );
		appendSql( valuesList.toString() );
		appendSql( ")" );
	}

	protected void renderMergeUpdate(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		if ( !valueBindings.isEmpty() ) {
			appendSql( "when matched then update set " );
			for ( int i = 0; i < valueBindings.size(); i++ ) {
				final ColumnValueBinding binding = valueBindings.get( i );
				if ( i > 0 ) {
					appendSql( ", " );
				}
				binding.getColumnReference().appendColumnForWrite( this, "t" );
				appendSql( "=" );
				binding.getColumnReference().appendColumnForWrite( this, "s" );
			}
			renderMatchedWhere( optimisticLockBindings );
		}
	}

	private void renderMatchedWhere(List<ColumnValueBinding> optimisticLockBindings) {
		if ( !optimisticLockBindings.isEmpty() ) {
			appendSql( " where " );
			for (int i = 0; i < optimisticLockBindings.size(); i++) {
				final ColumnValueBinding binding = optimisticLockBindings.get( i );
				if ( i>0 ) {
					appendSql(" and ");
				}
				binding.getColumnReference().appendColumnForWrite( this, "t" );
				appendSql("=");
				binding.getValueExpression().accept( this );
			}
		}
	}
}
