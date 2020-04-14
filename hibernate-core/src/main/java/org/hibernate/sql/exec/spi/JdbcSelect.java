/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

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
	private final List<FilterJdbcParameter> filterJdbcParameters;

	public JdbcSelect(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Set<String> affectedTableNames,
			List<FilterJdbcParameter> filterJdbcParameters) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.affectedTableNames = affectedTableNames;
		this.filterJdbcParameters = filterJdbcParameters;
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

	public JdbcValuesMappingProducer getJdbcValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}

	public void registerFilterJdbcParameterBindings(JdbcParameterBindings jdbcParameterBindings) {
		if ( CollectionHelper.isNotEmpty( filterJdbcParameters ) ) {
			for ( FilterJdbcParameter filterJdbcParameter : filterJdbcParameters ) {
				jdbcParameterBindings.addBinding( filterJdbcParameter.getParameter(), filterJdbcParameter.getBinding() );
			}
		}
	}
}
