/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported SQL AST translator base for PostgreSQL-derived databases.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class PostgreSQLFamilySqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {
	@SPI(IMPLEMENT)
	protected PostgreSQLFamilySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@SPI(IMPLEMENT)
	protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
		if ( context == TableReferenceAliasContext.INSERT_TARGET ) {
			appendSql( " as " );
			appendSql( alias );
		}
		else {
			super.renderTableReferenceAlias( alias, context );
		}
	}

	@Override
	protected Expectation expectation(OptionalTableUpdate optionalTableUpdate) {
		return optionalTableUpdate.getValueBindings().stream()
					.anyMatch( ColumnValueBinding::isAttributeUpdatable )
				// Never need to retry the statement, since the on conflict clause can never fail due to concurrency
				? new Expectation.RowCount()
				// Without updatable bindings, the merge affects 0 rows when matched
				: new Expectation.OptionalRowCount();
	}

	@Override
	protected void renderMergeStatement(OptionalTableUpdate optionalTableUpdate) {
		if ( optionalTableUpdate.getMutatingTable().isOptional() ) {
			super.renderMergeStatement( optionalTableUpdate );
		}
		else {
			applySqlComment( optionalTableUpdate.getMutationComment() );
			appendSql( "insert into " );

			appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
			registerAffectedTable( optionalTableUpdate.getMutatingTable().getTableName() );

			appendSql( ' ' );

			char separator = '(';
			boolean anyUpdatable = false;
			for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
				if ( valueBinding.isAttributeInsertable() ) {
					appendSql( separator );
					appendSql( valueBinding.getColumnReference().getColumnExpression() );
					separator = ',';
				}

				anyUpdatable |= valueBinding.isAttributeUpdatable();
			}
			for ( ColumnValueBinding keyBinding : optionalTableUpdate.getKeyBindings() ) {
				appendSql( separator );
				appendSql( keyBinding.getColumnReference().getColumnExpression() );
				separator = ',';
			}

			getCurrentClauseStack().push( Clause.VALUES );
			try {
				appendSql( ") values " );
				separator = '(';
				for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
					if ( valueBinding.isAttributeInsertable() ) {
						appendSql( separator );
						valueBinding.getValueExpression().accept( this );
						separator = ',';
					}
				}
				for ( ColumnValueBinding keyBinding : optionalTableUpdate.getKeyBindings() ) {
					appendSql( separator );
					keyBinding.getValueExpression().accept( this );
					separator = ',';
				}
			}
			finally {
				getCurrentClauseStack().pop();
			}

			appendSql( ") on conflict " );
			separator = '(';
			for ( ColumnValueBinding keyBinding : optionalTableUpdate.getKeyBindings() ) {
				appendSql( separator );
				appendSql( keyBinding.getColumnReference().getColumnExpression() );
				separator = ',';
			}
			appendSql( ')' );

			appendSql( " do " );

			if ( anyUpdatable ) {
				appendSql( "update set" );

				separator = ' ';
				for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
					if ( valueBinding.isAttributeUpdatable() ) {
						appendSql( separator );
						appendSql( valueBinding.getColumnReference().getColumnExpression() );
						appendSql( "=excluded." );
						appendSql( valueBinding.getColumnReference().getColumnExpression() );
						separator = ',';
					}
				}

				if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() != 0 ) {
					appendSql( " where" );
					String whereSeparator = " ";
					for ( ColumnValueBinding optimisticLockBinding : optionalTableUpdate.getOptimisticLockBindings() ) {
						appendSql( whereSeparator );
						if ( renderTenantRestriction( optimisticLockBinding, optionalTableUpdate.getTableName() ) ) {
							continue;
						}
						optimisticLockBinding.getColumnReference()
								.appendReadExpression( this, optionalTableUpdate.getTableName() );
						appendSql( "=" );
						optimisticLockBinding.getValueExpression().accept( this );
						whereSeparator = " and ";
					}
				}
			}
			else {
				appendSql( "nothing" );
			}
		}
	}
}
