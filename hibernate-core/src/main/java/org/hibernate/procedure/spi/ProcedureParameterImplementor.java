/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * SPI extension for ProcedureParameter
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0")
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, QueryParameterImplementor<T> {

	@Nonnull
	JdbcCallParameterRegistration toJdbcParameterRegistration(int startIndex, @Nonnull ProcedureCallImplementor<?> procedureCall);

}
