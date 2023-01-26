/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.MergeOperation;

/**
 * Base SqlAstTranslator for translators which support a full insert/update/delete MERGE statement
 *
 * @author Steve Ebersole
 */
public abstract class SqlAstTranslatorWithMerge<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	public SqlAstTranslatorWithMerge(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	/**
	 * Create the MutationOperation for performing a MERGE
	 */
	public MutationOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderMergeStatement( optionalTableUpdate );

		return new MergeOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				getParameterBinders()
		);
	}

	protected void renderMergeStatement(OptionalTableUpdate optionalTableUpdate) {
		// template:
		//
		// merge into [table] as t
		// using values([bindings]) as s ([column-names])
		// on t.[key] = s.[key]
		// when not matched
		// 		then insert ...
		// when matched
		//		and s.[columns] is null
		//		then delete
		// when matched
		//		then update set ...

		renderMergeInto( optionalTableUpdate );
		appendSql( " " );
		renderMergeUsing( optionalTableUpdate );
		appendSql( " " );
		renderMergeOn( optionalTableUpdate );
		appendSql( " " );
		renderMergeInsert( optionalTableUpdate );
		appendSql( " " );
		renderMergeDelete( optionalTableUpdate );
		appendSql( " " );
		renderMergeUpdate( optionalTableUpdate );
	}

	protected void renderMergeInto(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "merge into " );
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

	private void renderMergeSource(OptionalTableUpdate optionalTableUpdate) {
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
			renderCasted( keyBinding.getValueExpression() );
		}
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			appendSql( ", " );
			columnList.append( ", " );
			final ColumnValueBinding valueBinding = valueBindings.get( i );
			columnList.append( valueBinding.getColumnReference().getColumnExpression() );
			renderCasted( valueBinding.getValueExpression() );
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

	protected void renderMergeDelete(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();

		appendSql( " when matched " );
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

		appendSql( " when matched then update set " );
		for ( int i = 0; i < valueBindings.size(); i++ ) {
			final ColumnValueBinding binding = valueBindings.get( i );
			if ( i > 0 ) {
				appendSql( ", " );
			}
			binding.getColumnReference().appendColumnForWrite( this, "t" );
			appendSql( "=" );
			binding.getColumnReference().appendColumnForWrite( this, "s" );
		}
	}
}
