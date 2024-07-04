/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	protected void appendNameParameter(
			StringBuilder buffer,
			ProcedureParameterImplementor parameter,
			JdbcCallParameterRegistration registration) {
		buffer.append( parameter.getName() ).append( " => ?" );
	}

}
