/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;

import java.util.function.BooleanSupplier;

import static java.lang.Boolean.TRUE;

/// Support for {@linkplain org.hibernate.query.spi.QueryImplementor}
/// implementations based on SQM AST.
///
/// @author Steve Ebersole
public abstract class AbstractSqmQuery<R>
		extends AbstractQuery<R>
		implements SqmStatementAccess<R>, DomainQueryExecutionContext, InterpretationsKeySource {
	public static final String CRITERIA_HQL_STRING = "<criteria>";

	private transient Callback callback;

	public AbstractSqmQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	public AbstractSqmQuery(AbstractSqmQuery<R> original) {
		super( original.session );
	}

	protected void applyMementoOptions(NamedSqmQueryMemento<?> memento) {
		super.applyMementoOptions( memento );

		if ( memento instanceof NamedSelectionMemento<?> selectionMemento ) {
			if ( selectionMemento.getHibernateLockMode() != null ) {
				queryOptions.getLockOptions().setLockMode( selectionMemento.getHibernateLockMode() );
			}
			if ( selectionMemento.getPessimisticLockScope() != null  ) {
				queryOptions.getLockOptions().setLockScope( selectionMemento.getPessimisticLockScope() );
			}
			if ( selectionMemento.getLockTimeout() != null  ) {
				queryOptions.getLockOptions().setTimeout( selectionMemento.getLockTimeout() );
			}
			if ( selectionMemento.getFollowOnLockingStrategy() != null ) {
				queryOptions.getLockOptions().setFollowOnStrategy( selectionMemento.getFollowOnLockingStrategy() );
			}

			if ( selectionMemento.getFirstResult() != null ) {
				queryOptions.getLimit().setFirstRow( selectionMemento.getFirstResult() );
			}

			if ( selectionMemento.getMaxResults() != null ) {
				queryOptions.getLimit().setMaxRows( selectionMemento.getFirstResult() );
			}
		}

		if ( memento.getAnticipatedParameterTypes() != null ) {
			final var basicTypeRegistry = getTypeConfiguration().getBasicTypeRegistry();
			final var parameterMetadata = getParameterMetadata();
			memento.getAnticipatedParameterTypes().forEach( (key, value) ->
					parameterMetadata.getQueryParameter( key )
							.applyAnticipatedType( basicTypeRegistry.getRegisteredType( value ) ) );
		}
	}

	protected static void bindValueBindCriteriaParameters(
			DomainParameterXref domainParameterXref,
			QueryParameterBindings bindings) {
		for ( var entry : domainParameterXref.getQueryParameters().entrySet() ) {
			bindValueToCriteriaParameter( bindings, entry.getKey(),
					entry.getValue().get( 0 ) );
		}
	}

	private static <T> void bindValueToCriteriaParameter(
			QueryParameterBindings bindings,
			QueryParameterImplementor<?> queryParameterImplementor,
			SqmParameter<T> sqmParameter) {
		if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<T> wrapper ) {
			final var criteriaParameter = wrapper.getJpaCriteriaParameter();
			if ( criteriaParameter instanceof ValueBindJpaCriteriaParameter<T> ) {
				// Use the anticipated type for binding the value if possible
				bindings.getBinding( queryParameterImplementor )
						.setBindValue( criteriaParameter.getValue(),
								criteriaParameter.getAnticipatedType() );
			}
		}
	}

	@Override
	public Class<?> getResultType() {
		return null;
	}

	public abstract DomainParameterXref getDomainParameterXref();

	@Override
	public boolean isQueryPlanCacheable() {
		return queryOptions.getQueryPlanCachingEnabled() == TRUE;
	}

	@Override
	public QueryImplementor<R> setQueryPlanCacheable(boolean cachePlans) {
		queryOptions.setQueryPlanCachingEnabled( cachePlans );
		return this;
	}

	@Override
	protected void applyQueryPlanCachingHint(String hintName, Object value) {
		super.applyQueryPlanCachingHint( hintName, value );
	}

	@Override
	protected <P> QueryParameterImplementor<P> getQueryParameter(QueryParameterImplementor<P> parameter) {
		if ( parameter instanceof JpaCriteriaParameter<?> criteriaParameter ) {
			final var parameterWrapper = getDomainParameterXref()
					.getParameterResolutions()
					.getJpaCriteriaParamResolutions()
					.get( criteriaParameter );
			//noinspection unchecked
			return (QueryParameterImplementor<P>) getDomainParameterXref().getQueryParameter( parameterWrapper );
		}
		else {
			return super.getQueryParameter( parameter );
		}
	}

	@Override
	public int @Nullable [] unnamedParameterIndices() {
		return QueryHelper.unnamedParameterIndices( getDomainParameterXref() );
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return session.getLoadQueryInfluencers();
	}

	@Override
	public BooleanSupplier hasMultiValuedParameterBindingsChecker() {
		return this::hasMultiValuedParameterBindings;
	}

	protected boolean hasMultiValuedParameterBindings() {
		return getQueryParameterBindings().hasAnyMultiValuedBindings()
			|| getParameterMetadata().hasAnyMatching( QueryParameter::allowsMultiValuedBinding );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution infrastructure

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	protected void prepareForExecution() {
		// Reset the callback before every execution
		resetCallback();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainQueryExecutionContext

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public boolean hasCallbackActions() {
		return callback != null && callback.hasAfterLoadActions();
	}

	protected void resetCallback() {
		callback = null;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Unwrap

	@Override
	protected  <X> X unwrapDelegates(Class<X> type) {
		if ( type.isInstance( getDomainParameterXref() ) ) {
			//noinspection unchecked
			return (X) getDomainParameterXref();
		}
		return super.unwrapDelegates( type );
	}
}
