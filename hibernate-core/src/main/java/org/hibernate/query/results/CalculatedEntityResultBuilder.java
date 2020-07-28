/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * An entity DomainResult builder for cases when Hibernate implicitly
 * calculates the mapping
 *
 * @author Steve Ebersole
 */
public class CalculatedEntityResultBuilder implements ResultBuilder {
	private final String tableAlias;
	private final String entityName;
	private final LockMode explicitLockMode;

	public CalculatedEntityResultBuilder(
			String tableAlias,
			String entityName,
			LockMode explicitLockMode) {
		this.tableAlias = tableAlias;
		this.entityName = entityName;
		this.explicitLockMode = explicitLockMode;
	}

	@Override
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		return null;
	}
}
