/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
}
