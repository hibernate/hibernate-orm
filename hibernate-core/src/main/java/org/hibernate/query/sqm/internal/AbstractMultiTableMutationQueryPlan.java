/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;


/**
 * @since 7.1
 */
public abstract class AbstractMultiTableMutationQueryPlan<S extends SqmDmlStatement<?>, F> implements NonSelectQueryPlan {
	private final S statement;
	private final DomainParameterXref domainParameterXref;
	private final F strategy;

	private volatile MultiTableHandler handler;

	public AbstractMultiTableMutationQueryPlan(S statement, DomainParameterXref domainParameterXref, F strategy) {
		this.statement = statement;
		this.domainParameterXref = domainParameterXref;
		this.strategy = strategy;
	}

	protected abstract MultiTableHandlerBuildResult buildHandler(
			S statement,
			DomainParameterXref domainParameterXref,
			F strategy,
			DomainQueryExecutionContext context);

	@Override
	public int executeUpdate(DomainQueryExecutionContext context) {
		BulkOperationCleanupAction.schedule( context.getSession(), statement );
		final Interpretation interpretation = getInterpretation( context );
		return interpretation.handler().execute( interpretation.jdbcParameterBindings(), context );
	}

	// For Hibernate Reactive
	protected S getStatement() {
		return statement;
	}

	// For Hibernate Reactive
	protected Interpretation getInterpretation(DomainQueryExecutionContext context) {
		Interpretation builtInterpretation = null;
		MultiTableHandler localCopy = handler;

		if ( localCopy == null ) {
			synchronized (this) {
				localCopy = handler;
				if ( localCopy == null ) {
					final MultiTableHandlerBuildResult buildResult = buildHandler(
							statement,
							domainParameterXref,
							strategy,
							context
					);
					builtInterpretation = new Interpretation(
							buildResult.multiTableHandler(),
							buildResult.firstJdbcParameterBindings()
					);
					localCopy = buildResult.multiTableHandler();
					handler = localCopy;
				}
				else {
					builtInterpretation = updateInterpretation( localCopy, context );
				}
			}
		}
		else {
			builtInterpretation = updateInterpretation( localCopy, context );
		}
		return builtInterpretation != null ? builtInterpretation
				: new Interpretation( localCopy, localCopy.createJdbcParameterBindings( context ) );
	}

	private @Nullable Interpretation updateInterpretation(
			MultiTableHandler localCopy,
			DomainQueryExecutionContext context) {
		Interpretation builtInterpretation = null;

		// If the translation depends on parameter bindings or it isn't compatible with the current query options,
		// we have to rebuild the JdbcSelect, which is still better than having to translate from SQM to SQL AST again
		if ( localCopy.dependsOnParameterBindings() ) {
			final JdbcParameterBindings jdbcParameterBindings = localCopy.createJdbcParameterBindings( context );
			// If the translation depends on the limit or lock options, we have to rebuild the JdbcSelect
			// We could avoid this by putting the lock options into the cache key
			if ( !localCopy.isCompatibleWith( jdbcParameterBindings, context.getQueryOptions() ) ) {
				final MultiTableHandlerBuildResult buildResult = buildHandler(
						statement,
						domainParameterXref,
						strategy,
						context
				);
				localCopy = buildResult.multiTableHandler();
				builtInterpretation = new Interpretation(
						buildResult.multiTableHandler(),
						buildResult.firstJdbcParameterBindings()
				);
				handler = localCopy;
			}
			else {
				builtInterpretation = new Interpretation( localCopy, jdbcParameterBindings );
			}
		}
		return builtInterpretation;
	}

	// For Hibernate Reactive
	protected record Interpretation(
			MultiTableHandler handler,
			JdbcParameterBindings jdbcParameterBindings
	) {}

}
