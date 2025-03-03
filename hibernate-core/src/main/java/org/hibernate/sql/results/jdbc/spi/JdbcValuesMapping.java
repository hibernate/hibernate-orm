/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.ast.spi.SqlSelection;

/**
 * The "resolved" form of {@link JdbcValuesMappingProducer} providing access
 * to resolved JDBC results ({@link SqlSelection}) descriptors and resolved
 * domain results ({@link DomainResult}) descriptors.
 *
 * @see JdbcValuesMappingProducer#resolve
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesMapping {
	/**
	 * The JDBC selection descriptors.  Used to read ResultSet values and build
	 * the "JDBC values array"
	 */
	List<SqlSelection> getSqlSelections();

	int getRowSize();

	/**
	 * Mapping from value index to cache index.
	 */
	int[] getValueIndexesToCacheIndexes();

	/**
	 * The size of the row for caching.
	 */
	int getRowToCacheSize();

	List<DomainResult<?>> getDomainResults();

	JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor sessionFactory);

	LockMode determineDefaultLockMode(String alias, LockMode defaultLockMode);

}
