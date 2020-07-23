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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class EntityResultBuilder
		extends AbstractPropertyContainer<EntityResultBuilder>
		implements ResultBuilder, NativeQuery.RootReturn {
	private final String entityName;
	private final String tableAlias;

	private LockMode lockMode;

	private String discriminatorColumnAlias;

	public EntityResultBuilder(String entityName, String tableAlias) {
		this.entityName = entityName;
		this.tableAlias = tableAlias;
	}

	@Override
	protected String getPropertyBase() {
		return entityName;
	}

	@Override
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityResultBuilder setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public EntityResultBuilder setDiscriminatorAlias(String columnAlias) {
		this.discriminatorColumnAlias = columnAlias;
		return this;
	}
}
