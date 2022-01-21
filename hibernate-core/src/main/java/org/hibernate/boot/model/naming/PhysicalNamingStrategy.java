/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Pluggable strategy contract for applying physical naming rules for database object names.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PhysicalNamingStrategy {
	/**
	 * Determine the appropriate physical catalog name to use for the  given logical name
	 */
	Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the appropriate physical schema name to use for the given logical name
	 */
	Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the appropriate physical table name to use for the given logical name
	 */
	Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the appropriate physical sequence name to use for the given logical name
	 */
	Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the appropriate physical column name to use for the given logical name
	 */
	Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);
}
