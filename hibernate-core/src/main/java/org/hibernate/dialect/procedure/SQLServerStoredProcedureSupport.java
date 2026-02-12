/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;

public final class SQLServerStoredProcedureSupport extends AbstractStoredProcedureSupport {
	public static final SQLServerStoredProcedureSupport INSTANCE = new SQLServerStoredProcedureSupport( false );
	public static final SQLServerStoredProcedureSupport IF_EXISTS_INSTANCE = new SQLServerStoredProcedureSupport( true );

	private final boolean supportsIfExists;

	private SQLServerStoredProcedureSupport(boolean supportsIfExists) {
		this.supportsIfExists = supportsIfExists;
	}

	@Override
	public String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes) {
		return """
				create or alter procedure %s%s
					as
				begin
					%s;
				end
				""".formatted( name,
						parameterTypes.isEmpty()
								? ""
								: " " + renderParameterDeclarations( parameterTypes, "@p", " " ),
						replaceJdbcParameters( statement, index -> "@p" + index ) );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		return supportsIfExists
				? "drop procedure if exists " + name
				: "if object_id(N'" + name + "', N'P') is not null drop procedure " + name;
	}

}
