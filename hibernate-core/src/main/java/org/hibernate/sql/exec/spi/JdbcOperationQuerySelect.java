/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Executable JDBC command
 *
 * @author Steve Ebersole
 */
public class JdbcOperationQuerySelect extends AbstractJdbcOperationQuery {
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

	public JdbcValuesMappingProducer getJdbcValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}

	public int getRowsToSkip() {
		return rowsToSkip;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public boolean usesLimitParameters() {
		return offsetParameter != null || limitParameter != null;
	}

	public JdbcParameter getOffsetParameter() {
		return offsetParameter;
	}

	public JdbcParameter getLimitParameter() {
		return limitParameter;
	}

	public JdbcLockStrategy getLockStrategy() {
		return jdbcLockStrategy;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( !appliedParameters.isEmpty() ) {
			if ( jdbcParameterBindings == null ) {
				return false;
			}
			for ( var entry : appliedParameters.entrySet() ) {
				final JdbcParameter parameter = entry.getKey();
				final JdbcParameterBinding appliedBinding = entry.getValue();
				// This is a special case where the rendered SQL depends on the presence of the parameter,
				// but not specifically on the value. In this case we have to re-generate the SQL if we can't find a binding
				// The need for this can be tested with the OracleFollowOnLockingTest#testPessimisticLockWithMaxResultsThenNoFollowOnLocking
				// Since the Limit is not part of the query plan cache key, but this has an effect on follow on locking,
				// we must treat the absence of Limit parameters, when they were considered for locking, as incompatible
				if ( appliedBinding == null ) {
					if ( parameter == offsetParameter ) {
						if ( queryOptions.getLimit() == null || queryOptions.getLimit().getFirstRowJpa() == 0 ) {
							return false;
						}
					}
					else if ( parameter == limitParameter ) {
						if ( queryOptions.getLimit() == null || queryOptions.getLimit().getMaxRowsJpa() == Integer.MAX_VALUE ) {
							return false;
						}
					}
					else if ( jdbcParameterBindings.getBinding( parameter ) == null ) {
						return false;
					}
				}
				// We handle limit and offset parameters below
				if ( parameter != offsetParameter && parameter != limitParameter ) {
					final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( parameter );
					// TODO: appliedBinding can be null here, resulting in NPE
					if ( binding == null || !areEqualBindings( appliedBinding, binding ) ) {
						return false;
					}
				}
			}
		}
		final Limit limit = queryOptions.getLimit();
		return ( offsetParameter != null || limitParameter != null || limit == null || limit.isEmpty() )
			&& isCompatible( offsetParameter, limit == null ? null : limit.getFirstRow(), 0 )
			&& isCompatible( limitParameter, limit == null ? null : limit.getMaxRows(), Integer.MAX_VALUE );
	}

	private static boolean areEqualBindings(JdbcParameterBinding appliedBinding, JdbcParameterBinding binding) {
		final JavaType<Object> javaTypeDescriptor = appliedBinding.getBindType().getJavaTypeDescriptor();
		return javaTypeDescriptor.areEqual( binding.getBindValue(), appliedBinding.getBindValue() );
	}

	private boolean isCompatible(JdbcParameter parameter, Integer requestedValue, int defaultValue) {
		if ( parameter == null ) {
			return requestedValue == null;
		}
		else {
			final JdbcParameterBinding jdbcParameterBinding = appliedParameters.get( parameter );
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
