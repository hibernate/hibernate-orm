package org.hibernate.query.sqm.internal;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;


/**
 * @since 7.1
 */
public abstract class AbstractMultiTableMutationQueryPlan<S extends SqmDmlStatement<?>, F> implements NonSelectQueryPlan {
	private final S statement;
	private final DomainParameterXref domainParameterXref;
	private final F strategy;

	private record CachedHandler(MultiTableHandler handler, DomainParameterXref parameters) {}

	private volatile CachedHandler cachedHandler;

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
		var localCopy = cachedHandler;
		if ( localCopy == null || !localCopy.parameters.matches( context.getQueryParameterBindings() ) ) {
			synchronized ( this ) {
				localCopy = cachedHandler;
				if ( localCopy == null || !localCopy.parameters.matches( context.getQueryParameterBindings() ) ) {
					return buildInterpretation( context );
				}
			}
		}
		final var handler = localCopy.handler;
		final var jdbcParameterBindings = handler.createJdbcParameterBindings( context );
		if ( handler.dependsOnParameterBindings()
				&& !handler.isCompatibleWith( jdbcParameterBindings, context.getQueryOptions() ) ) {
			return buildInterpretation( context );
		}
		return new Interpretation( handler, jdbcParameterBindings );
	}

	private Interpretation buildInterpretation(DomainQueryExecutionContext context) {
		final var parameters = domainParameterXref.forTranslation();
		final var build = buildHandler( statement, parameters, strategy, context );
		cachedHandler = new CachedHandler( build.multiTableHandler(), parameters );
		return new Interpretation( build.multiTableHandler(), build.firstJdbcParameterBindings() );
	}

	// For Hibernate Reactive
	protected record Interpretation(
			MultiTableHandler handler,
			JdbcParameterBindings jdbcParameterBindings
	) {}

}
