/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.FunctionOutput;
import org.hibernate.procedure.FunctionReturn;
import org.hibernate.result.Output;
import org.hibernate.result.internal.ResultSetOutputImpl;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl implements FunctionReturn {
	private final int sqlType;
	private final JdbcCallFunctionReturn jdbcFunctionReturn;

	public FunctionReturnImpl(int sqlType) {
		this.jdbcFunctionReturn = new JdbcCallFunctionReturnImpl( sqlType );
		this.sqlType = sqlType;
	}

	public JdbcCallFunctionReturn toJdbcFunctionReturn() {
		return jdbcFunctionReturn;
	}

	@Override
	public Output extractOutput(CallableStatement callableStatement, SharedSessionContractImplementor session) {
		final Object functionReturnValue = jdbcFunctionReturn.extractOutput( callableStatement, session );

		if ( sqlType == Types.REF_CURSOR ) {
			return new ResultSetOutputImpl( extractQueryResults( (ResultSet) functionReturnValue ) );
		}
		else {
			return new FunctionOutput( functionReturnValue );
		}
	}

	private List extractQueryResults(ResultSet resultSet) {
		throw new NotYetImplementedException(  );
	}
}
