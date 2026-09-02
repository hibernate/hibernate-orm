/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.SPI;

/// Factory for the standard already-rendered SQL locking strategies.
///
/// These strategies own placement only. Clause and hint syntax is always
/// delegated to the corresponding focused renderer.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public final class StandardLockingSqlRewriters {
	private StandardLockingSqlRewriters() {
	}

	/// Selects the standard raw-SQL strategy for a cohesive locking profile.
	public static LockingSqlRewriter forSupport(LockingSupport lockingSupport) {
		Objects.requireNonNull( lockingSupport, "lockingSupport" );
		return switch ( lockingSupport.getMetadata().getPessimisticLockStyle() ) {
			case CLAUSE -> statementSuffix( lockingSupport.getLockingClauseRenderer() );
			case TABLE_HINT -> tableHints( lockingSupport.getTableLockHintRenderer() );
			case NONE -> StandardLockingSqlRewriters::unsupportedOrNotApplicable;
		};
	}

	/// Creates a strategy which appends a rendered locking clause.
	public static LockingSqlRewriter statementSuffix(LockingClauseRenderer renderer) {
		Objects.requireNonNull( renderer, "renderer" );
		return request -> applyClause( request, renderer, false );
	}

	/// Creates a strategy which prepends a rendered locking clause.
	public static LockingSqlRewriter statementPrefix(LockingClauseRenderer renderer) {
		Objects.requireNonNull( renderer, "renderer" );
		return request -> applyClause( request, renderer, true );
	}

	/// Creates a strategy which inserts rendered hints after known table aliases.
	public static LockingSqlRewriter tableHints(TableLockHintRenderer renderer) {
		Objects.requireNonNull( renderer, "renderer" );
		return request -> applyTableHints( request, renderer );
	}

	private static LockingSqlRewriteResult applyClause(
			LockingSqlRewriteRequest request,
			LockingClauseRenderer renderer,
			boolean prefix) {
		if ( request.lockKind() == PessimisticLockKind.NONE ) {
			return LockingSqlRewriteResult.notApplicable( request.sql() );
		}
		final String fragment = Objects.requireNonNull(
				renderer.render( new LockingClauseRequest(
						request.lockKind(),
						request.timeout(),
						request.targets()
				) ),
				"locking clause renderer result"
		);
		if ( fragment.isEmpty() ) {
			return LockingSqlRewriteResult.unsupported( request.sql() );
		}
		return LockingSqlRewriteResult.applied(
				prefix ? fragment + " " + request.sql() : request.sql() + fragment
		);
	}

	private static LockingSqlRewriteResult applyTableHints(
			LockingSqlRewriteRequest request,
			TableLockHintRenderer renderer) {
		if ( request.lockKind() == PessimisticLockKind.NONE ) {
			return LockingSqlRewriteResult.notApplicable( request.sql() );
		}

		final Set<String> tableAliases = new LinkedHashSet<>();
		for ( LockingClauseRequest.Target target : request.targets() ) {
			if ( target instanceof LockingClauseRequest.TableTarget tableTarget ) {
				tableAliases.add( tableTarget.tableAlias() );
			}
			else if ( target instanceof LockingClauseRequest.ColumnTarget columnTarget ) {
				tableAliases.add( columnTarget.tableAlias() );
			}
		}
		if ( tableAliases.isEmpty() ) {
			return LockingSqlRewriteResult.unsupported( request.sql() );
		}

		final StringBuilder rewrittenSql = new StringBuilder( request.sql() );
		for ( String tableAlias : tableAliases ) {
			final int aliasStart = findAlias( rewrittenSql, tableAlias );
			if ( aliasStart < 0 ) {
				return LockingSqlRewriteResult.unsupported( request.sql() );
			}
			final String hint = Objects.requireNonNull(
					renderer.render( new HintRequest(
							request.lockKind(),
							request.timeout(),
							tableAlias
					) ),
					"table lock hint renderer result"
			);
			if ( hint.isEmpty() ) {
				return LockingSqlRewriteResult.unsupported( request.sql() );
			}
			rewrittenSql.insert( aliasStart + tableAlias.length(), hint );
		}
		return LockingSqlRewriteResult.applied( rewrittenSql.toString() );
	}

	private static int findAlias(CharSequence sql, String tableAlias) {
		for ( int index = 0; index <= sql.length() - tableAlias.length(); index++ ) {
			if ( matchesAliasAt( sql, tableAlias, index ) ) {
				return index;
			}
		}
		return -1;
	}

	private static boolean matchesAliasAt(CharSequence sql, String tableAlias, int index) {
		if ( index > 0 && !isAliasBoundary( sql.charAt( index - 1 ) ) ) {
			return false;
		}
		for ( int i = 0; i < tableAlias.length(); i++ ) {
			if ( sql.charAt( index + i ) != tableAlias.charAt( i ) ) {
				return false;
			}
		}
		final int end = index + tableAlias.length();
		return end == sql.length() || isAliasBoundary( sql.charAt( end ) );
	}

	private static boolean isAliasBoundary(char character) {
		return Character.isWhitespace( character ) || character == ',' || character == ')' || character == ';';
	}

	private static LockingSqlRewriteResult unsupportedOrNotApplicable(LockingSqlRewriteRequest request) {
		return request.lockKind() == PessimisticLockKind.NONE
				? LockingSqlRewriteResult.notApplicable( request.sql() )
				: LockingSqlRewriteResult.unsupported( request.sql() );
	}

	private record HintRequest(
			PessimisticLockKind lockKind,
			jakarta.persistence.Timeout timeout,
			String tableExpression) implements TableLockHintRequest {
	}
}
