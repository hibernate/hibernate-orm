/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;

/**
 * Standard implementation of JdbcParameters
 *
 * @author Steve Ebersole
 */
public class JdbcParametersImpl implements JdbcParameters {

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
