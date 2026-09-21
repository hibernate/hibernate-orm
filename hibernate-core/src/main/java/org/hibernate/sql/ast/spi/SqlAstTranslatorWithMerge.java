/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.MergeOperation;

/**
 * Base for translators which support a full insert-or-update-or-delete (MERGE) command.
 * <p>
 * Use {@link #createMergeOperation(OptionalTableUpdate)} to translate an
 * {@linkplain OptionalTableUpdate} into an executable {@linkplain MergeOperation}
 * operation.
 *
 * @author Steve Ebersole
 */
public abstract class SqlAstTranslatorWithMerge<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	public SqlAstTranslatorWithMerge(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	/**
	 * Create the MutationOperation for performing a MERGE.
	 * <p>
	 * The OptionalTableUpdate is {@linkplain #renderMergeStatement translated}
	 * and wrapped as a MutationOperation
	 */
	public MergeOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderMergeStatement( optionalTableUpdate );
		return new MergeOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				expectation( optionalTableUpdate ),
				getParameterBinders()
		);
	}

	protected Expectation expectation(OptionalTableUpdate optionalTableUpdate) {
		return mergeExpectation( optionalTableUpdate );
	}

	private static Expectation mergeExpectation(OptionalTableUpdate optionalTableUpdate) {
		return optionalTableUpdate.getValueBindings().stream()
					.anyMatch( ColumnValueBinding::isAttributeUpdatable )
				? optionalTableUpdate.getMutatingTable().isOptional()
					// When the mutating table is optional, we would generate a delete part for the merge statement
					// which makes the statement non-idempotent and hence not retryable
					? new Expectation.RowCount()
					: new Expectation.RetryableRowCount()
				// Without updatable bindings, the merge affects 0 rows when matched
				: new Expectation.OptionalRowCount();
	}

	/**
	 * Renders the OptionalTableUpdate as a MERGE query.
	 *
	 */
	protected void renderMergeStatement(OptionalTableUpdate optionalTableUpdate) {
		//
		// merge into <target-table> as t
		// using (select col_1, col_2, ... from dual) as s
		// on (t.key = s.key)
		// when not matched
		//	 then insert ...
		// when matched
		//      and s.col_1 is null
		//	    and s.col_2 is null
		//		and ...
		//   then delete
		// when matched
		//   then update ...

		// `merge into <target-table> [as] t`
		renderMergeInto( optionalTableUpdate );
		appendSql( ' ' );

		// using (select col_1, col_2, ... from dual) as s
		renderMergeUsing( optionalTableUpdate );
		appendSql( ' ' );

		// on (t.key = s.key)
		renderMergeOn( optionalTableUpdate );
		appendSql( ' ' );

		// when not matched
		//	 then insert ...
		renderMergeInsert( optionalTableUpdate );
		appendSql( ' ' );

		if ( optionalTableUpdate.getMutatingTable().isOptional() ) {
			// when matched
			//      and s.col_1 is null
			//	    and s.col_2 is null
			//		and ...
			//   then delete
			renderMergeDelete( optionalTableUpdate );
			appendSql( ' ' );
		}

		// when matched
		//   then update ...
		renderMergeUpdate( optionalTableUpdate );
	}

	protected void renderMergeInto(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "merge into " );
		renderMergeTarget( optionalTableUpdate );
	}

	private void renderMergeTarget(OptionalTableUpdate optionalTableUpdate) {
		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		appendSql( " " );
		renderMergeTargetAlias();
	}

	protected void renderMergeTargetAlias() {
		appendSql( "as t" );
	}

	protected void renderMergeUsing(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "using (" );
		renderMergeUsingQuery( optionalTableUpdate );
		appendSql( ") " );

		renderMergeSourceAlias();
	}

	protected void renderMergeSourceAlias() {
		appendSql( "as s" );
	}

	protected void renderMergeUsingQuery(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();

		appendSql( "select" );

		char separator = ' ';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			renderMergeUsingQuerySelection( keyBinding );
			separator = ',';
		}
		for ( ColumnValueBinding valueBinding : valueBindings ) {
			appendSql( ',' );
			renderMergeUsingQuerySelection( valueBinding );
		}

		final String selectionTable = StringHelper.nullIfEmpty( getFromDualForSelectOnly() );
		if ( selectionTable != null ) {
			appendSql( ' ' );
			appendSql( selectionTable );
		}
	}

	protected void renderMergeUsingQuerySelection(ColumnValueBinding selectionBinding) {
		renderColumnWrite( selectionBinding );
		appendSql( ' ' );
		appendSql( selectionBinding.getColumnReference().getColumnExpression() );
	}

	protected void renderMergeOn(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "on (" );

		String separator = "";
		for ( ColumnValueBinding keyBinding : optionalTableUpdate.getKeyBindings() ) {
			appendSql( separator );
			keyBinding.getColumnReference().appendReadExpression( this, "t" );
			appendSql( '=' );
			keyBinding.getColumnReference().appendReadExpression( this, "s" );
			separator = " and ";
		}

		appendSql( ')' );
	}

	protected void renderMergeInsert(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();

		appendSql( "when not matched then insert " );
		char separator = '(';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			appendSql( keyBinding.getColumnReference().getColumnExpression() );
			separator = ',';
		}
		for ( ColumnValueBinding valueBinding : valueBindings ) {
			if ( valueBinding.isAttributeInsertable() ) {
				appendSql( ',' );
				appendSql( valueBinding.getColumnReference().getColumnExpression() );
			}
		}

		appendSql( ") values " );
		separator = '(';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			keyBinding.getColumnReference().appendReadExpression( this, "s" );
			separator = ',';
		}
		for ( ColumnValueBinding valueBinding : valueBindings ) {
			if ( valueBinding.isAttributeInsertable() ) {
				appendSql( ',' );
				valueBinding.getColumnReference().appendReadExpression( this, "s" );
			}
		}
		appendSql( ')' );
	}

	protected void renderMergeDelete(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		renderWhenMatched( optimisticLockBindings );
		for ( ColumnValueBinding binding : valueBindings ) {
			appendSql( " and " );
			binding.getColumnReference().appendReadExpression( this, "s" );
			appendSql( " is null" );
		}
		appendSql( " then delete" );
	}

	protected void renderMergeUpdate(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		if ( valueBindings.stream().anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			renderWhenMatched( optimisticLockBindings );
			appendSql( " then update set" );
			char separator = ' ';
			for ( ColumnValueBinding binding : valueBindings ) {
				if ( binding.isAttributeUpdatable() ) {
					appendSql( separator );
					binding.getColumnReference().appendColumnForWrite( this, null );
					appendSql( "=" );
					binding.getColumnReference().appendColumnForWrite( this, "s" );
					separator = ',';
				}
			}
		}
	}

	private void renderWhenMatched(List<ColumnValueBinding> optimisticLockBindings) {
		appendSql( "when matched" );
		for ( ColumnValueBinding binding : optimisticLockBindings ) {
			appendSql( " and " );
			if ( renderTenantRestriction( binding, "t" ) ) {
				continue;
			}
			binding.getColumnReference().appendReadExpression( this, "t" );
			appendSql( "=" );
			binding.getValueExpression().accept( this );
		}
	}
}
