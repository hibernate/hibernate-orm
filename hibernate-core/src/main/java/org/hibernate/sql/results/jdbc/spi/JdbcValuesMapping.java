/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

	List<DomainResult<?>> getDomainResults();

	JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor sessionFactory);

	LockMode determineDefaultLockMode(String alias, LockMode defaultLockMode);

}
