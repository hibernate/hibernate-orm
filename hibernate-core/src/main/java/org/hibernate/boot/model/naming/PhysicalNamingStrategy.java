/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Pluggable strategy contract for applying physical naming rules for database object names.
 *
 * NOTE: Ideally we'd pass "extra" things in here like Dialect, etc to better handle identifier
 * length constraints or auto quoting of identifiers.  However, the pre-metamodel model does not
 * necessarily know this information at the time the strategy is called.
 *
 * @author Steve Ebersole
 */
public interface PhysicalNamingStrategy {
	public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment);

	public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment);

	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment);

	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment);

	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment);
}
