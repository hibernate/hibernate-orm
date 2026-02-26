/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.util.List;

/**
 * Default implementation of {@link StoredProcedureSupport}
 * for dialects that support stored procedures.
 */
public final class NoStoredProcedureSupport implements StoredProcedureSupport {
	public static final NoStoredProcedureSupport INSTANCE = new NoStoredProcedureSupport();

	private NoStoredProcedureSupport() {
	}

	@Override
	public boolean supportsStoredProcedures() {
		return false;
	}

	@Override
	public String parameterTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String resultTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String mutationInvocationSql(String name, int parameterCount) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String selectInvocationSql(String name, int parameterCount) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public boolean isSelectCallable() {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String createMutationProcedureDdl(String name, String statement, List<String> parameterTypes) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String dropMutationProcedureDdl(String name) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String createSelectProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes,
			List<String> resultTypeNames,
			List<String> resultColumnNames) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public String dropSelectProcedureDdl(String name, List<String> parameterTypes) {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

	@Override
	public boolean requiresSelectResultDescriptor() {
		throw new UnsupportedOperationException( "This dialect does not support stored procedures" );
	}

}
