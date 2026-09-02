/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.spi.mutation.jdbc.MergeOperation;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported provider base for translators which implement a full
/// insert-or-update-or-delete `MERGE` command.
///
/// Use [#createMergeOperation] to translate an [OptionalTableUpdate] into an
/// executable [MergeOperation]. That method owns the operation-construction
/// lifecycle. A provider customizes SQL grammar through the protected
/// `renderMerge...` callbacks.
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class SqlAstTranslatorWithMerge<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	/// Creates a full-MERGE translator for one translation request.
	@SPI(IMPLEMENT)
	protected SqlAstTranslatorWithMerge(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	/// Creates the mutation operation for performing a full `MERGE`.
	///
	/// The optional-table update is [#renderMergeStatement translated] and
	/// wrapped in a [MergeOperation]. Override a protected rendering callback to
	/// customize the SQL; the operation lifecycle itself is invariant.
	public final MergeOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderMergeStatement( optionalTableUpdate );
		return new MergeOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				expectation( optionalTableUpdate ),
				getParameterBinders()
		);
	}

	private static Expectation expectation(OptionalTableUpdate optionalTableUpdate) {
		return optionalTableUpdate.getValueBindings().stream()
					.anyMatch( ColumnValueBinding::isAttributeUpdatable )
				? new Expectation.RowCount()
				// Without updatable bindings, the merge affects 0 rows when matched
				: new Expectation.OptionalRowCount();
	}

	/// Renders the complete optional-table update as a `MERGE` statement.
	///
	/// Override this only when the statement structure cannot be expressed by a
	/// more focused callback, and preserve the order of its insert, optional
	/// delete, and update arms.
	@SPI(IMPLEMENT)
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
		appendSql( " " );

		// using (select col_1, col_2, ... from dual) as s
		renderMergeUsing( optionalTableUpdate );
		appendSql( " " );

		// on (t.key = s.key)
		renderMergeOn( optionalTableUpdate );
		appendSql( " " );

		// when not matched
		//	 then insert ...
		renderMergeInsert( optionalTableUpdate );
		appendSql( " " );

		if ( optionalTableUpdate.getMutatingTable().isOptional() ) {
			// when matched
			//      and s.col_1 is null
			//	    and s.col_2 is null
			//		and ...
			//   then delete
			renderMergeDelete( optionalTableUpdate );
			appendSql( " " );
		}

		// when matched
		//   then update ...
		renderMergeUpdate( optionalTableUpdate );
	}

	/// Renders the `merge into` clause and target table.
	@SPI(IMPLEMENT)
	protected void renderMergeInto(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "merge into " );
		renderMergeTarget( optionalTableUpdate );
	}

	private void renderMergeTarget(OptionalTableUpdate optionalTableUpdate) {
		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		appendSql( " " );
		renderMergeTargetAlias();
	}

	/// Renders the target-table alias, including any required `as` keyword.
	@SPI(IMPLEMENT)
	protected void renderMergeTargetAlias() {
		appendSql( "as t" );
	}

	/// Renders the complete `using` clause around the generated source query.
	@SPI(IMPLEMENT)
	protected void renderMergeUsing(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "using (" );
		renderMergeUsingQuery( optionalTableUpdate );
		appendSql( ") " );

		renderMergeSourceAlias();
	}

	/// Renders the source-query alias, including any required `as` keyword.
	@SPI(IMPLEMENT)
	protected void renderMergeSourceAlias() {
		appendSql( "as s" );
	}

	private void renderMergeUsingQuery(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();

		appendSql( "select " );

		for ( int i = 0; i < keyBindings.size(); i++ ) {
			if ( i > 0 ) {
				appendSql( ", " );
			}
			renderMergeUsingQuerySelection( keyBindings.get( i ) );
		}
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			appendSql( ", " );
			renderMergeUsingQuerySelection( valueBindings.get( i ) );
		}

		final String selectionTable = StringHelper.nullIfEmpty( getSelectOnlyFromClause() );
		if ( selectionTable != null ) {
			appendSql( " " );
			appendSql( selectionTable );
		}
	}

	/// Renders one selection and alias in the generated `using` query.
	@SPI(IMPLEMENT)
	protected void renderMergeUsingQuerySelection(ColumnValueBinding selectionBinding) {
		renderColumnWrite( selectionBinding );
		appendSql( " " );
		appendSql( selectionBinding.getColumnReference().getColumnExpression() );
	}

	/// Renders the key-matching `on` predicate.
	@SPI(IMPLEMENT)
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

	/// Renders the `when not matched` insert arm.
	@SPI(IMPLEMENT)
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
			final ColumnValueBinding valueBinding = valueBindings.get( i );
			if ( valueBinding.isAttributeInsertable() ) {
				appendSql( ", " );
				valuesList.append( ", " );
				appendSql( valueBinding.getColumnReference().getColumnExpression() );
				valueBinding.getColumnReference().appendReadExpression( "s", valuesList::append );
			}
		}

		appendSql( ") values (" );
		appendSql( valuesList.toString() );
		appendSql( ")" );
	}

	/// Renders the `when matched` delete arm for an optional table.
	@SPI(IMPLEMENT)
	protected void renderMergeDelete(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		renderWhenMatched( optimisticLockBindings );
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			final ColumnValueBinding binding = valueBindings.get( i );
			appendSql( " and " );
			binding.getColumnReference().appendReadExpression( this, "s" );
			appendSql( " is null" );
		}
		appendSql( " then delete" );
	}

	/// Renders the `when matched` update arm when updatable bindings exist.
	@SPI(IMPLEMENT)
	protected void renderMergeUpdate(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		if ( valueBindings.stream().anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			renderWhenMatched( optimisticLockBindings );
			appendSql( " then update set " );
			boolean first = true;
			for ( int i = 0; i < valueBindings.size(); i++ ) {
				final ColumnValueBinding binding = valueBindings.get( i );
				if ( binding.isAttributeUpdatable() ) {
					if ( first ) {
						first = false;
					}
					else {
						appendSql( ", " );
					}
					binding.getColumnReference().appendColumnForWrite( this, null );
					appendSql( "=" );
					binding.getColumnReference().appendColumnForWrite( this, "s" );
				}
			}
		}
	}

	private void renderWhenMatched(List<ColumnValueBinding> optimisticLockBindings) {
		appendSql( "when matched" );
		for (int i = 0; i < optimisticLockBindings.size(); i++) {
			final ColumnValueBinding binding = optimisticLockBindings.get( i );
			appendSql(" and ");
			binding.getColumnReference().appendColumnForWrite( this, "t" );
			appendSql("<=");
			binding.getValueExpression().accept( this );
		}
	}
}
