/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.metamodel.mapping.SqlTypedMapping;

/**
 * JdbcParameter for temporal restrictions; bound via JdbcParameterBindings.
 *
 * @author Gavin King
 */
public class TemporalJdbcParameter extends SqlTypedMappingJdbcParameter {

	public TemporalJdbcParameter(SqlTypedMapping sqlTypedMapping) {
		super( sqlTypedMapping );
	}
}
