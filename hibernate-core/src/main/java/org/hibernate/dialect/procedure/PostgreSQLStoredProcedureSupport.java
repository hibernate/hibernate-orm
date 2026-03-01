/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;

public final class PostgreSQLStoredProcedureSupport extends AbstractStoredProcedureSupport {
	public static final PostgreSQLStoredProcedureSupport INSTANCE = new PostgreSQLStoredProcedureSupport();

	private PostgreSQLStoredProcedureSupport() {
	}

	@Override
	public String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes) {
		return
				"""
				create or replace procedure %s(%s)
					language plpgsql
					as
				$$
				begin
					execute '%s'%s;
				end;
				$$"""
				.formatted( name,
						renderParameterDeclarations( parameterTypes, "in p", " " ),
						replaceJdbcParameters( statement, index -> "$" + index )
								.replace( "'", "''" ),
						parameterTypes.isEmpty()
								? ""
								: " using " + renderCommaSeparated( parameterTypes.size(), i -> "p" + i ) );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		return "drop procedure if exists " + name;
	}

	@Override
	public String selectInvocationSql(String name, int parameterCount) {
		return "select * from " + name + renderCallParameters( parameterCount );
	}

	@Override
	public boolean isSelectCallable() {
		return false;
	}

	@Override
	public String createSelectProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes,
			List<String> resultTypeNames,
			List<String> resultColumnNames) {
		return
				"""
				create or replace function %s(%s)
					returns table(%s)
					language sql
					as
				$$
					%s
				$$""".formatted( name,
						renderParameterDeclarations( parameterTypes, "p", " " ),
						renderCommaSeparated( resultTypeNames.size(),
								i -> resultColumnNames.get( i - 1 ) + " " + resultTypeNames.get( i - 1 ) ),
						replaceJdbcParameters( statement, index -> "$" + index ) );
	}

	@Override
	public String dropSelectProcedureDdl(String name, List<String> parameterTypes) {
		return "drop function if exists %s(%s)"
				.formatted( name, String.join( ", ", parameterTypes ) );
	}

	@Override
	public boolean requiresSelectResultDescriptor() {
		return true;
	}

}
