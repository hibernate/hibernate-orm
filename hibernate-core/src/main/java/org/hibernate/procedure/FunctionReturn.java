/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.Incubating;

/**
 * Describes the function return value of a {@link ProcedureCall}
 * executed via a JDBC escape of form ({@code "{? = call ...}}.
 * That is, the {@code ?} parameter occurring before the {@code =}.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface FunctionReturn<T> extends ProcedureParameter<T> {
	/**
	 * The {@linkplain org.hibernate.type.SqlTypes JDBC type code}
	 * representing the SQL type of the function return value.
	 */
	int getJdbcTypeCode();
}
