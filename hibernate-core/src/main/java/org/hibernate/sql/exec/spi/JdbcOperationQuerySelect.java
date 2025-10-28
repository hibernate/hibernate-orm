/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

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

	/**
	 * @deprecated {@code filterJdbcParameters} is no longer used
	 */
	@Deprecated
	public JdbcOperationQuerySelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			Set<FilterJdbcParameter> filterJdbcParameters) {
		this(
				sql,
				parameterBinders,
				jdbcValuesMappingProducer,
				affectedTableNames
		);
	}

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
				null,
				0,
				Integer.MAX_VALUE,
				Collections.emptyMap(),
				JdbcLockStrategy.AUTO,
				null,
				null
		);
	}

	/**
	 * @deprecated {@code filterJdbcParameters} is no longer used
	 */
	@Deprecated
	public JdbcOperationQuerySelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			Set<FilterJdbcParameter> filterJdbcParameters,
			int rowsToSkip,
			int maxRows,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters,
			JdbcLockStrategy jdbcLockStrategy,
			JdbcParameter offsetParameter,
			JdbcParameter limitParameter) {
		this(
				sql,
				parameterBinders,
				jdbcValuesMappingProducer,
				affectedTableNames,
				rowsToSkip,
				maxRows,
				appliedParameters,
				jdbcLockStrategy,
				offsetParameter,
				limitParameter
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
		if ( !super.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		final Limit limit = queryOptions.getLimit();
		if ( offsetParameter == null && limitParameter == null ) {
			if ( limit != null && !limit.isEmpty() ) {
				return false;
			}
		}
		if ( !isCompatible( offsetParameter, limit == null ? null : limit.getFirstRow(), 0 ) ) {
			return false;
		}
		if ( !isCompatible( limitParameter, limit == null ? null : limit.getMaxRows(), Integer.MAX_VALUE ) ) {
			return false;
		}
		return true;
	}

	@Override
	protected boolean isCompatible(
			JdbcParameter parameter,
			JdbcParameterBinding appliedBinding,
			JdbcParameterBinding binding,
			QueryOptions queryOptions) {
		// This is a special case where the rendered SQL depends on the presence of the parameter,
		// but not specifically on the value. In this case we have to re-generate the SQL if we can't find a binding
		// The need for this can be tested with the OracleFollowOnLockingTest#testPessimisticLockWithMaxResultsThenNoFollowOnLocking
		// Since the Limit is not part of the query plan cache key, but this has an effect on follow on locking,
		// we must treat the absence of Limit parameters, when they were considered for locking, as incompatible.
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
		}
		// We handle limit and offset parameters above and in isCompatibleWith
		if ( parameter != offsetParameter && parameter != limitParameter ) {
			return super.isCompatible( parameter, appliedBinding, binding, queryOptions );
		}
		return true;
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
				final int value;
				if ( requestedValue == null ) {
					value = defaultValue;
				}
				else {
					value = requestedValue;
				}
				return value == (int) jdbcParameterBinding.getBindValue();
			}
		}
	}
}
