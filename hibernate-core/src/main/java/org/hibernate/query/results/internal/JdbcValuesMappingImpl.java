/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockMode;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.internal.StandardJdbcValuesMapping;

import java.util.List;
import java.util.Map;

/**
 * JdbcValuesMapping implementation used while building
 * {@linkplain org.hibernate.query.results.ResultSetMapping} references.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingImpl extends StandardJdbcValuesMapping {
	private final int rowSize;
	private final @Nullable Map<String, LockMode> registeredLockModes;

	public JdbcValuesMappingImpl(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults,
			int rowSize,
			@Nullable Map<String, LockMode> registeredLockModes) {
		super( sqlSelections, domainResults );
		this.rowSize = rowSize;
		this.registeredLockModes = registeredLockModes;
	}

	@Override
	public int getRowSize() {
		return rowSize;
	}

	@Override
	public LockMode determineDefaultLockMode(String alias, LockMode defaultLockMode) {
		return registeredLockModes == null ? defaultLockMode : registeredLockModes.getOrDefault( alias, defaultLockMode );
	}
}
