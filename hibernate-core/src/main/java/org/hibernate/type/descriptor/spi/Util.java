/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class Util {
	private Util() {
	}

	public static CallableStatement asCallableStatement(PreparedStatement stmnt, Supplier<String> errorMessageSupplier) {
		// atm we only support this for CallableStatements...
		if ( !(stmnt instanceof CallableStatement ) ) {
			throw new UnsupportedOperationException( errorMessageSupplier.get() );
		}

		return (CallableStatement) stmnt;
	}

	public static CallableStatement asCallableStatementForNamedParam(PreparedStatement stmnt) {
		return Util.asCallableStatement(
				stmnt,
				() -> "No support for named parameter binding except to CallableStatements : " + stmnt
		);
	}
}
