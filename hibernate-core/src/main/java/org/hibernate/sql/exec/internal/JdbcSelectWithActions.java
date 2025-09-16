/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcOperationQuery;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.SecondaryAction;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectWithActions implements JdbcOperationQuery, JdbcSelect {
	private final JdbcOperationQuerySelect primaryOperation;

	private final LoadedValuesCollector loadedValuesCollector;
	private final PreAction[] preActions;
	private final PostAction[] postActions;

	public JdbcSelectWithActions(
			JdbcOperationQuerySelect primaryOperation,
			LoadedValuesCollector loadedValuesCollector,
			PreAction[] preActions,
			PostAction[] postActions) {
		this.primaryOperation = primaryOperation;
		this.loadedValuesCollector = loadedValuesCollector;
		this.preActions = preActions;
		this.postActions = postActions;
	}

	public JdbcSelectWithActions(
			JdbcOperationQuerySelect primaryAction,
			LoadedValuesCollector loadedValuesCollector) {
		this( primaryAction, loadedValuesCollector, null, null );
	}

	@Override
	public JdbcValuesMappingProducer getJdbcValuesMappingProducer() {
		return primaryOperation.getJdbcValuesMappingProducer();
	}

	@Override
	public JdbcLockStrategy getLockStrategy() {
		return primaryOperation.getLockStrategy();
	}

	@Override
	public boolean usesLimitParameters() {
		return primaryOperation.usesLimitParameters();
	}

	@Override
	public JdbcParameter getLimitParameter() {
		return primaryOperation.getLimitParameter();
	}

	@Override
	public int getRowsToSkip() {
		return primaryOperation.getRowsToSkip();
	}

	@Override
	public int getMaxRows() {
		return primaryOperation.getMaxRows();
	}

	@Override
	public @Nullable LoadedValuesCollector getLoadedValuesCollector() {
		return loadedValuesCollector;
	}

	@Override
	public void performPreActions(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
		if ( preActions == null ) {
			return;
		}

		for ( int i = 0; i < preActions.length; i++ ) {
			preActions[i].performPreAction( jdbcStatementAccess, jdbcConnection, executionContext );
		}
	}

	@Override
	public void performPostAction(boolean succeeded, StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
		if ( postActions == null ) {
			return;
		}

		for ( int i = 0; i < postActions.length; i++ ) {
			if ( succeeded || postActions[i].shouldRunAfterFail() ) {
				postActions[i].performPostAction( jdbcStatementAccess, jdbcConnection, executionContext );
			}
		}
	}

	@Override
	public Set<String> getAffectedTableNames() {
		// NOTE: the complete set of affected table-names might be
		// slightly expanded here accounting for pre- and post-actions
		return primaryOperation.getAffectedTableNames();
	}

	@Override
	public String getSqlString() {
		return primaryOperation.getSqlString();
	}

	@Override
	public List<JdbcParameterBinder> getParameterBinders() {
		return primaryOperation.getParameterBinders();
	}

	@Override
	public boolean dependsOnParameterBindings() {
		return primaryOperation.dependsOnParameterBindings();
	}

	@Override
	public Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters() {
		return primaryOperation.getAppliedParameters();
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		// todo : is this enough here?
		return primaryOperation.isCompatibleWith( jdbcParameterBindings, queryOptions );
	}

	public static class Builder {
		private final JdbcOperationQuerySelect primaryAction;

		private LoadedValuesCollector loadedValuesCollector;
		protected List<PreAction> preActions;
		protected List<PostAction> postActions;

		public Builder(JdbcOperationQuerySelect primaryAction) {
			this.primaryAction = primaryAction;
		}

		public Builder setLoadedValuesCollector(LoadedValuesCollector loadedValuesCollector) {
			this.loadedValuesCollector = loadedValuesCollector;
			return this;
		}

		public JdbcSelectWithActions build() {
			if ( preActions == null && postActions == null ) {
				return new JdbcSelectWithActions( primaryAction, loadedValuesCollector );
			}
			final PreAction[] preActions = toPreActionArray( this.preActions );
			final PostAction[] postActions = toPostActionArray( this.postActions );
			return new JdbcSelectWithActions( primaryAction, loadedValuesCollector, preActions, postActions );
		}

		/**
		 * Appends the {@code actions} to the growing list of pre-actions,
		 * executed (in order) after all currently registered actions.
		 *
		 * @return {@code this}, for method chaining.
		 */
		public Builder appendPreAction(PreAction... actions) {
			if ( preActions == null ) {
				preActions = new ArrayList<>();
			}
			Collections.addAll( preActions, actions );
			return this;
		}

		/**
		 * Prepends the {@code actions} to the growing list of pre-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public Builder prependPreAction(PreAction... actions) {
			if ( preActions == null ) {
				preActions = new ArrayList<>();
			}
			// todo (DatabaseOperation) : should we invert the order of the incoming actions?
			Collections.addAll( preActions, actions );
			return this;
		}

		/**
		 * Appends the {@code actions} to the growing list of post-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public Builder appendPostAction(PostAction... actions) {
			if ( postActions == null ) {
				postActions = new ArrayList<>();
			}
			Collections.addAll( postActions, actions );
			return this;
		}

		/**
		 * Prepends the {@code actions} to the growing list of post-actions
		 *
		 * @return {@code this}, for method chaining.
		 */
		public Builder prependPostAction(PostAction... actions) {
			if ( postActions == null ) {
				postActions = new ArrayList<>();
			}
			// todo (DatabaseOperation) : should we invert the order of the incoming actions?
			Collections.addAll( postActions, actions );
			return this;
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
		public Builder addSecondaryActionPair(SecondaryAction action) {
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
		public Builder addSecondaryActionPair(PreAction preAction, PostAction postAction) {
			prependPreAction( preAction );
			appendPostAction( postAction );
			return this;
		}

		private static PreAction[] toPreActionArray(List<PreAction> actions) {
			if ( CollectionHelper.isEmpty( actions ) ) {
				return null;
			}
			return actions.toArray( new PreAction[0] );
		}

		private static PostAction[] toPostActionArray(List<PostAction> actions) {
			if ( CollectionHelper.isEmpty( actions ) ) {
				return null;
			}
			return actions.toArray( new PostAction[0] );
		}
	}
}
