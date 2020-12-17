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

import org.hibernate.LockOptions;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Executable JDBC command
 *
 * @author Steve Ebersole
 */
public class JdbcSelect implements JdbcOperation {
	private final String sql;
	private final List<JdbcParameterBinder> parameterBinders;
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;
	private final Set<String> affectedTableNames;
	private final Set<FilterJdbcParameter> filterJdbcParameters;
	private final int rowsToSkip;
	private final int maxRows;
	private final Map<JdbcParameter, JdbcParameterBinding> appliedParameters;
	private final LockOptions appliedLockOptions;
	private final JdbcParameter offsetParameter;
	private final JdbcParameter limitParameter;

	public JdbcSelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			Set<FilterJdbcParameter> filterJdbcParameters) {
		this(
				sql,
				parameterBinders,
				jdbcValuesMappingProducer,
				affectedTableNames,
				filterJdbcParameters,
				0,
				Integer.MAX_VALUE,
				Collections.emptyMap(),
				null,
				null,
				null
		);
	}

	public JdbcSelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			Set<FilterJdbcParameter> filterJdbcParameters,
			int rowsToSkip,
			int maxRows,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters,
			LockOptions appliedLockOptions,
			JdbcParameter offsetParameter,
			JdbcParameter limitParameter) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.affectedTableNames = affectedTableNames;
		this.filterJdbcParameters = filterJdbcParameters;
		this.rowsToSkip = rowsToSkip;
		this.maxRows = maxRows;
		this.appliedParameters = appliedParameters;
		this.appliedLockOptions = appliedLockOptions;
		this.offsetParameter = offsetParameter;
		this.limitParameter = limitParameter;
	}

	@Override
	public String getSql() {
		return sql;
	}

	@Override
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	public Set<FilterJdbcParameter> getFilterJdbcParameters() {
		return filterJdbcParameters;
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

	@Override
	public boolean dependsOnParameterBindings() {
		return !appliedParameters.isEmpty();
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( !appliedParameters.isEmpty() ) {
			if ( jdbcParameterBindings == null ) {
				return false;
			}
			for ( Map.Entry<JdbcParameter, JdbcParameterBinding> entry : appliedParameters.entrySet() ) {
				final JdbcParameter parameter = entry.getKey();
				// We handle limit and offset parameters below
				if ( parameter != offsetParameter && parameter != limitParameter ) {
					final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( entry.getKey() );
					final JdbcParameterBinding appliedBinding = entry.getValue();
					if ( binding == null || !appliedBinding.getBindType()
							.getJavaTypeDescriptor()
							.areEqual( binding.getBindValue(), appliedBinding.getBindValue() ) ) {
						return false;
					}
				}
			}
		}
		final LockOptions lockOptions = queryOptions.getLockOptions();
		if ( appliedLockOptions == null ) {
			if ( lockOptions != null && !lockOptions.isEmpty() ) {
				return false;
			}
		}
		else if ( !appliedLockOptions.isCompatible( lockOptions ) ) {
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

	private boolean isCompatible(JdbcParameter parameter, Integer requestedValue, int defaultValue) {
		final int value;
		if ( requestedValue == null ) {
			value = defaultValue;
		}
		else {
			value = requestedValue;
		}
		if ( parameter != null ) {
			final JdbcParameterBinding jdbcParameterBinding = appliedParameters.get( parameter );
			if ( jdbcParameterBinding != null ) {
				if ( value != (int) jdbcParameterBinding.getBindValue() ) {
					return false;
				}
			}
		}
		else if ( value != defaultValue ) {
			return false;
		}
		return true;
	}
}
