/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;

/**
 * Standard implementation of JdbcParameters
 *
 * @author Steve Ebersole
 */
public class JdbcParametersImpl implements JdbcParameters {
	/**
	 * Singleton access
	 */
	public static final JdbcParametersImpl NO_PARAMETERS = new JdbcParametersImpl();

	private Set<JdbcParameter> jdbcParameters;

	@Override
	public void addParameter(JdbcParameter parameter) {
		if ( jdbcParameters == null ) {
			jdbcParameters = new HashSet<>();
		}

		jdbcParameters.add( parameter );
	}

	@Override
	public void addParameters(Collection<JdbcParameter> parameters) {
		if ( jdbcParameters == null ) {
			jdbcParameters = new HashSet<>();
		}

		jdbcParameters.addAll( parameters );
	}

	@Override
	public Set<JdbcParameter> getJdbcParameters() {
		return jdbcParameters == null ? Collections.emptySet() : jdbcParameters;
	}
}
