/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.DatabaseOperation;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.SecondaryAction;
import org.hibernate.sql.exec.spi.StatementAccess;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract support for DatabaseOperation implementations, mainly
 * managing {@linkplain PreAction pre-} and {@linkplain PostAction post-}
 * actions.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDatabaseOperation<P extends JdbcOperation>
		implements DatabaseOperation<P> {
	protected final PreAction[] preActions;
	protected final PostAction[] postActions;

	@SuppressWarnings("unused")
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

	protected static PreAction[] toPreActionArray(List<PreAction> actions) {
		if ( CollectionHelper.isEmpty( actions ) ) {
			return null;
		}
		return actions.toArray( new PreAction[0] );
	}

	protected static PostAction[] toPostActionArray(List<PostAction> actions) {
		if ( CollectionHelper.isEmpty( actions ) ) {
			return null;
		}
		return actions.toArray( new PostAction[0] );
	}

	protected abstract static class Builder<T extends Builder<T>> {
		protected List<PreAction> preActions;
		protected List<PostAction> postActions;

		protected abstract T getThis();

		/**
		 * Appends the {@code actions} to the growing list of pre-actions,
		 * executed (in order) after all currently registered actions.
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
			// todo (DatabaseOperation) : should we invert the order of the incoming actions?
			Collections.addAll( preActions, actions );
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
			// todo (DatabaseOperation) : should we invert the order of the incoming actions?
			Collections.addAll( postActions, actions );
			return getThis();
		}

		/**
		 * Adds a secondary action pair.
		 * Assumes the {@code action} implements both {@linkplain PreAction} and {@linkplain PostAction}.
		 *
		 * @apiNote Prefer {@linkplain #addSecondaryActionPair(PreAction, PostAction)} to avoid
		 * the casts needed here.
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
