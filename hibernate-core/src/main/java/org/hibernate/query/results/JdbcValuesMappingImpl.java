/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.internal.StandardJdbcValuesMapping;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of JdbcValuesMapping for native / procedure queries
 *
 * @author Steve Ebersole
 */
@Internal
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
