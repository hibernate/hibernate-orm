/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.procedure;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Dialect-specific stored-procedure DDL rendering used by
 * {@link org.hibernate.engine.jdbc.procedure.StoredProcedureHelper}.
 *
 * @author Gavin King
 */
@Incubating
public interface StoredProcedureSupport {

	boolean supportsStoredProcedures();

	boolean isSelectCallable();

	String parameterTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect);

	String resultTypeName(JdbcMapping jdbcMapping, DdlTypeRegistry registry, Dialect dialect);

	String mutationInvocationSql(String name, int parameterCount);

	String selectInvocationSql(String name, int parameterCount);

	String createMutationProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes);

	String dropMutationProcedureDdl(String name);

	String createSelectProcedureDdl(
			String name,
			String statement,
			List<String> parameterTypes,
			List<String> resultTypeNames,
			List<String> resultColumnNames);

	String dropSelectProcedureDdl(String name, List<String> parameterTypes);

	boolean requiresSelectResultDescriptor();
}
