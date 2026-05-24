/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.Incubating;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * SPI extension for ProcedureParameter
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, QueryParameterImplementor<T> {

	JdbcCallParameterRegistration toJdbcParameterRegistration(int startIndex, ProcedureCallImplementor<?> procedureCall);

}
