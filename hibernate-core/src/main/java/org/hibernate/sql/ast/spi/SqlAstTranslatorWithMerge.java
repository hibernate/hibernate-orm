/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.MergeOperation;

/**
 * Base for translators which support a full insert-or-update-or-delete (MERGE) command.
 * <p/>
 * Use {@link #createMergeOperation(OptionalTableUpdate)} to translate an
 * {@linkplain OptionalTableUpdate} into an executable {@linkplain MergeOperation}
 * operation.
 * <p/>
 *
 *
 * @author Steve Ebersole
 */
public abstract class SqlAstTranslatorWithMerge<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	@Deprecated(forRemoval = true, since = "7.1")
	public SqlAstTranslatorWithMerge(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	public SqlAstTranslatorWithMerge(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable SqlParameterInfo parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
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
				getParameterBinders()
		);
	}

//	@Override
//	public void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
//		renderMergeStatement(tableUpdate);
//	}
//
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

		final String selectionTable = StringHelper.nullIfEmpty( getFromDualForSelectOnly() );
		if ( selectionTable != null ) {
			appendSql( " " );
			appendSql( selectionTable );
		}
	}

	protected void renderMergeUsingQuerySelection(ColumnValueBinding selectionBinding) {
		renderCasted( selectionBinding.getValueExpression() );
		appendSql( " " );
		appendSql( selectionBinding.getColumnReference().getColumnExpression() );
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

	protected void renderMergeUpdate(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();
		final List<ColumnValueBinding> optimisticLockBindings = optionalTableUpdate.getOptimisticLockBindings();

		renderWhenMatched( optimisticLockBindings );
		appendSql( " then update set " );
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			final ColumnValueBinding binding = valueBindings.get( i );
			if ( i > 0 ) {
				appendSql( ", " );
			}
			binding.getColumnReference().appendColumnForWrite( this, null );
			appendSql( "=" );
			binding.getColumnReference().appendColumnForWrite( this, "s" );
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
