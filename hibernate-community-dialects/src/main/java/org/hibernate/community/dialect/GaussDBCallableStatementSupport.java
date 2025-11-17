/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * GaussDB implementation of CallableStatementSupport.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on OracleCallableStatementSupport.
 */
public class GaussDBCallableStatementSupport extends StandardCallableStatementSupport {

	public static final StandardCallableStatementSupport INSTANCE = new GaussDBCallableStatementSupport(
			true );

	private GaussDBCallableStatementSupport(boolean supportsProcedures) {
		super( supportsProcedures );
	}

	@Override
	protected void appendNameParameter(
			StringBuilder buffer,
			ProcedureParameterImplementor parameter,
			JdbcCallParameterRegistration registration) {
		buffer.append( parameter.getName() ).append( " => ?" );
	}
}
