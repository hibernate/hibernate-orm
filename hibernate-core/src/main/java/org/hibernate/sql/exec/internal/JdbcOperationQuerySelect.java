/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executable JDBC command produced from some form of Query.
 *
 * @author Steve Ebersole
 */
public class JdbcOperationQuerySelect
		extends AbstractJdbcOperationQuery
		implements JdbcSelect {
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;
	private final int rowsToSkip;
	private final int maxRows;
	private final JdbcParameter offsetParameter;
	private final JdbcParameter limitParameter;
	private final JdbcLockStrategy jdbcLockStrategy;

	public JdbcOperationQuerySelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames) {
		this(
				sql,
				parameterBinders,
				jdbcValuesMappingProducer,
				affectedTableNames,
				0,
				Integer.MAX_VALUE,
				Collections.emptyMap(),
				JdbcLockStrategy.AUTO,
				null,
				null
		);
	}

	public JdbcOperationQuerySelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			int rowsToSkip,
			int maxRows,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters,
			JdbcLockStrategy jdbcLockStrategy,
			JdbcParameter offsetParameter,
			JdbcParameter limitParameter) {
		super( sql, parameterBinders, affectedTableNames, appliedParameters );
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.rowsToSkip = rowsToSkip;
		this.maxRows = maxRows;
		this.jdbcLockStrategy = jdbcLockStrategy;
		this.offsetParameter = offsetParameter;
		this.limitParameter = limitParameter;
	}

	@Override
	public JdbcValuesMappingProducer getJdbcValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}

	@Override
	public int getRowsToSkip() {
		return rowsToSkip;
	}

	@Override
	public int getMaxRows() {
		return maxRows;
	}

	@Override
	public @Nullable LoadedValuesCollector getLoadedValuesCollector() {
		return null;
	}

	@Override
	public void performPreActions(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
	}

	@Override
	public void performPostAction(boolean succeeded, StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
	}

	@Override
	public boolean usesLimitParameters() {
		return offsetParameter != null || limitParameter != null;
	}

	@Override
	public JdbcParameter getLimitParameter() {
		return limitParameter;
	}

	public JdbcParameter getOffsetParameter() {
		return offsetParameter;
	}

	@Override
	public JdbcLockStrategy getLockStrategy() {
		return jdbcLockStrategy;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		final var limit = queryOptions.getLimit();
		if ( !appliedParameters.isEmpty() ) {
			if ( jdbcParameterBindings == null ) {
				return false;
			}
			for ( var entry : appliedParameters.entrySet() ) {
				final var parameter = entry.getKey();
				final var appliedBinding = entry.getValue();
				// This is a special case where the rendered SQL depends on the presence of the parameter,
				// but not specifically on the value. In this case we have to re-generate the SQL if we can't find a binding
				// The need for this can be tested with the OracleFollowOnLockingTest#testPessimisticLockWithMaxResultsThenNoFollowOnLocking
				// Since the Limit is not part of the query plan cache key, but this has an effect on follow on locking,
				// we must treat the absence of Limit parameters, when they were considered for locking, as incompatible
				if ( appliedBinding == null ) {
					if ( parameter == offsetParameter ) {
						if ( limit == null || limit.getFirstRowJpa() == 0 ) {
							return false;
						}
					}
					else if ( parameter == limitParameter ) {
						if ( limit == null || limit.getMaxRowsJpa() == Integer.MAX_VALUE ) {
							return false;
						}
					}
					else if ( jdbcParameterBindings.getBinding( parameter ) == null ) {
						return false;
					}
				}
				// We handle limit and offset parameters below
				if ( parameter != offsetParameter && parameter != limitParameter ) {
					final var binding = jdbcParameterBindings.getBinding( parameter );
					// TODO: appliedBinding can be null here, resulting in NPE
					if ( binding == null
							|| !equal( appliedBinding, binding, appliedBinding.getBindType().getJavaTypeDescriptor() ) ) {
						return false;
					}
				}
			}
		}
		return ( offsetParameter != null || limitParameter != null || limit == null || limit.isEmpty() )
			&& isCompatible( offsetParameter, limit == null ? null : limit.getFirstRow(), 0 )
			&& isCompatible( limitParameter, limit == null ? null : limit.getMaxRows(), Integer.MAX_VALUE );
	}

	private boolean isCompatible(JdbcParameter parameter, Integer requestedValue, int defaultValue) {
		if ( parameter == null ) {
			return requestedValue == null;
		}
		else {
			final var jdbcParameterBinding = appliedParameters.get( parameter );
			if ( jdbcParameterBinding == null ) {
				// If this query includes the parameter this is only compatible when a requested value is given through the query options
				// If not, this query string contains limit/offset but the query options don't request that
				// Considering this case compatible would lead to binding null for limit/offset which is invalid
				// todo (6.0): maybe it's better if the presence/absence is part of the cache key the cache?
				//  org.hibernate.query.internal.QueryInterpretationCacheStandardImpl.hqlInterpretationCache
				return requestedValue != null;
			}
			else {
				final int value = requestedValue == null ? defaultValue : requestedValue;
				return value == (int) jdbcParameterBinding.getBindValue();
			}
		}
	}
}
