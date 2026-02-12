/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.util.List;

public final class OracleStoredProcedureSupport extends AbstractStoredProcedureSupport {
	public static final OracleStoredProcedureSupport INSTANCE = new OracleStoredProcedureSupport();

	private OracleStoredProcedureSupport() {
	}

	@Override
	public String parameterTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		final String rawTypeName =
				registry.getDescriptor( jdbcMapping.getJdbcType().getDdlTypeCode() )
						.getRawTypeName();
		// CLOB parameters cannot be used reliably in scalar predicates like "=" in PL/SQL.
		return "clob".equals( rawTypeName )
			|| "nclob".equals( rawTypeName )
				? "varchar2"
				: rawTypeName;
	}

	@Override
	public String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes) {
		return """
				create or replace procedure %s%s
					as
				begin
					%s;
				end;
				""".formatted( name,
						parameterTypes.isEmpty()
								? ""
								: "(" + renderParameterDeclarations( parameterTypes, "p", " in " ) + ")",
						replaceJdbcParameters( statement, index -> "p" + index ) );
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
				create or replace procedure %s%s
					as hibernate_result_cursor sys_refcursor;
				begin
					open hibernate_result_cursor for %s;
					dbms_sql.return_result(hibernate_result_cursor);
				end;
				""".formatted( name,
						parameterTypes.isEmpty()
								? ""
								: "(" + renderParameterDeclarations( parameterTypes, "p", " in " ) + ")",
						replaceJdbcParameters( statement, index -> "p" + index ) );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		return
				"""
				begin
					execute immediate 'drop procedure %s';
					exception when others then
						if sqlcode != -4043 then raise;
						end if;
				end;
				""".formatted( name );
	}
}
