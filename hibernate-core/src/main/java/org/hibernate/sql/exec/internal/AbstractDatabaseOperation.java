/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.DatabaseOperation;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.SecondaryAction;
import org.hibernate.sql.exec.spi.StatementAccess;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ALL")
public abstract class AbstractDatabaseOperation implements DatabaseOperation {
	protected final PreAction[] preActions;
	protected final PostAction[] postActions;

	public AbstractDatabaseOperation() {
		this( null, null );
	}

	public AbstractDatabaseOperation(PreAction[] preActions, PostAction[] postActions) {
		this.preActions = preActions;
		this.postActions = postActions;
	}

	protected void performPreActions(
			StatementAccess statementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		if ( preActions != null ) {
			for ( int i = 0; i < preActions.length; i++ ) {
				preActions[i].performPreAction( statementAccess, jdbcConnection, executionContext );
			}
		}
	}

	protected void performPostActions(
			StatementAccess statementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		if ( postActions != null ) {
			for ( int i = 0; i < postActions.length; i++ ) {
				postActions[i].performPostAction( statementAccess, jdbcConnection, executionContext );
			}
		}
	}

	protected static <T extends SecondaryAction> T[] toArray(Class<T> type, List<T> actions) {
		if ( CollectionHelper.isEmpty( actions ) ) {
			return null;
		}
		return actions.toArray( (T[]) Array.newInstance( type, 0 ) );
	}

	protected abstract static class Builder<T extends Builder<T>> {
		protected List<PreAction> preActions;
		protected List<PostAction> postActions;

		protected abstract T getThis();

		/**
		 * Appends the {@code actions} to the growing list of pre-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T appendPreAction(PreAction... actions) {
			if ( preActions == null ) {
				preActions = new ArrayList<>();
			}
			Collections.addAll( preActions, actions );
			return getThis();
		}

		/**
		 * Prepends the {@code actions} to the growing list of pre-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T prependPreAction(PreAction... actions) {
			if ( preActions == null ) {
				preActions = new ArrayList<>();
			}
			for ( int i = actions.length - 1; i >= 0; i-- ) {
				preActions.add( 0, actions[i] );
			}
			return getThis();
		}

		/**
		 * Appends the {@code actions} to the growing list of post-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T appendPostAction(PostAction... actions) {
			if ( postActions == null ) {
				postActions = new ArrayList<>();
			}
			Collections.addAll( postActions, actions );
			return getThis();
		}

		/**
		 * Prepends the {@code actions} to the growing list of post-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T prependPostAction(PostAction... actions) {
			if ( postActions == null ) {
				postActions = new ArrayList<>();
			}
			for ( int i = actions.length - 1; i >= 0; i-- ) {
				postActions.add( 0, actions[i] );
			}
			return getThis();
		}

		/**
		 * Adds a secondary action.  Assumes the action implements both
		 * {@linkplain PreAction} and {@linkplain PostAction}.
		 *
		 * @see #prependPreAction
		 * @see #appendPostAction
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T addSecondaryActionPair(SecondaryAction action) {
			return addSecondaryActionPair( (PreAction) action, (PostAction) action );
		}

		/**
		 * Adds a PreAction/PostAction pair.
		 *
		 * @see #prependPreAction
		 * @see #appendPostAction
		 *
		 * @return {@code this}, for method chaining.
		 */
		public T addSecondaryActionPair(PreAction preAction, PostAction postAction) {
			prependPreAction( preAction );
			appendPostAction( postAction );
			return getThis();
		}
	}
}
