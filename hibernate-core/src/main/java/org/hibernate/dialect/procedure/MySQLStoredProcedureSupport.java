/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;

public final class MySQLStoredProcedureSupport extends AbstractStoredProcedureSupport {
	public static final MySQLStoredProcedureSupport INSTANCE = new MySQLStoredProcedureSupport();

	private MySQLStoredProcedureSupport() {
	}

	@Override
	public String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes) {
		//TODO: Maria supports 'or replace'
		return
				"""
				create procedure %s(%s)
				begin
					%s;
				end
				""".formatted( name,
						renderParameterDeclarations( parameterTypes, "in p", " " ),
						replaceJdbcParameters( statement, index -> "p" + index ) );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		return "drop procedure if exists " + name;
	}

}
