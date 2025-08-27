/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * Standard implementation of {@link org.hibernate.procedure.spi.CallableStatementSupport}.
 *
 * @author Steve Ebersole
 */
public class OracleCallableStatementSupport extends StandardCallableStatementSupport {

	public static final StandardCallableStatementSupport REF_CURSOR_INSTANCE = new OracleCallableStatementSupport( true );


	public OracleCallableStatementSupport(boolean supportsRefCursors) {
		super( supportsRefCursors );
	}

	@Override
	protected void appendNameParameter(
			StringBuilder buffer,
			ProcedureParameterImplementor<?> parameter,
			JdbcCallParameterRegistration registration) {
		buffer.append( parameter.getName() ).append( " => ?" );
	}

}
