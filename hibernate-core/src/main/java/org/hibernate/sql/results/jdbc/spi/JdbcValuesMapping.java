/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;

/**
 * The "resolved" form of {@link JdbcValuesMappingProducer} providing access
 * to resolved JDBC results ({@link SqlSelection}) descriptors and resolved
 * domain results ({@link DomainResult}) descriptors.
 *
 * @see JdbcValuesMappingProducer#resolve
 * @see JdbcValuesMappingProducer#resolve(JdbcValuesMetadata, org.hibernate.engine.spi.LoadQueryInfluencers, SessionFactoryImplementor)
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
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

	/// Resolves and supplies the assembler and initializer plan.
	///
	/// @see JdbcValuesMappingResolution
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor sessionFactory);

	LockMode determineDefaultLockMode(String alias, LockMode defaultLockMode);

}
