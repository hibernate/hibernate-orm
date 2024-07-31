/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

public class SQLServerCallableStatementSupport extends StandardCallableStatementSupport {

	public static final StandardCallableStatementSupport INSTANCE = new SQLServerCallableStatementSupport( );


	private SQLServerCallableStatementSupport() {
		super( false );
	}

	@Override
	protected void appendNameParameter(StringBuilder buffer, ProcedureParameterImplementor parameter, JdbcCallParameterRegistration registration) {
		buffer.append( '@' ).append( parameter.getName() ).append( " = ?" );
	}
}
