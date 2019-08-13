/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

/**
 * Generator for SqlAliasBase instances based on a stem.
 *
 * @author Steve Ebersole
 */
public interface SqlAliasBaseGenerator {
	/**
	 * Generate the SqlAliasBase based on the given stem.
	 */
	SqlAliasBase createSqlAliasBase(String stem);
}
