/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import org.hibernate.type.mapper.spi.Type;

/**
 * Models the actual call, allowing iterative building of the parts.
 *
 * @author Steve Ebersole
 */
public class JdbcCall {
	private final ProcedureCallImpl procedureCall;
	private final boolean useFunctionCallSyntax;

	private int jdbcParameterCount;


	private boolean firstParameter = true;

	public JdbcCall(ProcedureCallImpl procedureCall, boolean useFunctionCallSyntax) {
		this.procedureCall = procedureCall;
		this.useFunctionCallSyntax = useFunctionCallSyntax;
	}

	public void registerParameters(Type hibernateType) {
		if ( firstParameter ) {
			firstParameter = false;

			if ( useFunctionCallSyntax  ) {
				if ( hibernateType.getColumnSpan() > 1 ) {
					throw new IllegalArgumentException( "Function return cannot be a composite type" );
				}
			}

			jdbcParameterCount += hibernateType.getColumnSpan();
		}
	}

	public String toCallString() {
		final StringBuilder buffer = new StringBuilder();

		final int argumentParameterStartIndex;

		if ( useFunctionCallSyntax  ) {
			buffer.append( "{?=call " ).append( procedureCall.getProcedureName() ).append( "(" );
			argumentParameterStartIndex = 1;
		}
		else {
			argumentParameterStartIndex = 0;
			buffer.append( "{call " ).append( procedureCall.getProcedureName() ).append( "(" );
		}

		boolean firstArgument = true;
		for ( int i = argumentParameterStartIndex; i < jdbcParameterCount; i++ ) {
			if ( firstArgument ) {
				firstArgument = false;
			}
			else {
				buffer.append( ',' );
			}
			buffer.append( '?' );
		}

		buffer.append( ")}" );

		return buffer.toString();
	}
}
