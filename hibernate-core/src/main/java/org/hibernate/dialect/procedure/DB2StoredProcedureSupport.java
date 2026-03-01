/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

public final class DB2StoredProcedureSupport extends AbstractStoredProcedureSupport {
	public static final DB2StoredProcedureSupport INSTANCE = new DB2StoredProcedureSupport();

	private DB2StoredProcedureSupport() {
	}

	@Override
	public String parameterTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		final String rawTypeName =
				registry.getDescriptor( jdbcMapping.getJdbcType().getDdlTypeCode() )
						.getRawTypeName();
		return "clob".equals( rawTypeName )
			|| "nclob".equals( rawTypeName )
				? "varchar(" + dialect.getMaxVarcharLength() + ")"
				: rawTypeName;
	}

	@Override
	public String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes) {
		return
				"""
				create or replace procedure %s(%s)
					language sql
				begin
					%s;
				end
				""".formatted( name,
						renderParameterDeclarations( parameterTypes, "in p", " " ),
						replaceJdbcParameters( statement, index -> "p" + index ) );
	}

	@Override
	public String selectInvocationSql(String name, int parameterCount) {
		return "select * from table(" + name + renderCallParameters( parameterCount ) + ")";
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
				return %s
				""".formatted( name,
						renderParameterDeclarations( parameterTypes, "p", " " ),
						renderCommaSeparated( resultTypeNames.size(),
								i -> resultColumnNames.get( i - 1 ) + " " + resultTypeNames.get( i - 1 ) ),
						replaceJdbcParameters( statement, index -> "p" + index ) );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		return
				"""
				begin
					declare continue handler for sqlstate '42704' begin end;
					execute immediate 'drop procedure %s';
				end;
				""".formatted( name );
	}

	@Override
	public String dropSelectProcedureDdl(String name, List<String> parameterTypes) {
//		final String function =
//				parameterTypes.isEmpty()
//						? name
//						: name + "(" + String.join( ", ", parameterTypes ) + ")";
		return
				"""
				begin
					declare continue handler for sqlstate '42704' begin end;
					execute immediate 'drop function %s';
				end;
				""".formatted( name );

	}

	@Override
	public boolean requiresSelectResultDescriptor() {
		return true;
	}
}
