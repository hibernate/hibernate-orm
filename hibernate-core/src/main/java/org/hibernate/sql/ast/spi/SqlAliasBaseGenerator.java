/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
