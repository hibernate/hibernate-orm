/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * @author Steve Ebersole
 */
public class JdbcParameterImpl extends AbstractJdbcParameter {

	public JdbcParameterImpl(JdbcMapping jdbcMapping) {
		super( jdbcMapping );
	}

	public JdbcParameterImpl(JdbcMapping jdbcMapping, Integer parameterId) {
		super( jdbcMapping, parameterId );
	}
}
