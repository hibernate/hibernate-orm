/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Models the qualified name of a database object.  Some things to keep in
 * mind wrt catalog/schema:<ol>
 *     <li>{@link java.sql.DatabaseMetaData#isCatalogAtStart}</li>
 *     <li>{@link java.sql.DatabaseMetaData#getCatalogSeparator()}</li>
 * </ol>
 * <p>
 * Also, be careful about the usage of {@link #render}.  If the intention is to get the name
 * as used in the database, the {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment} to
 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format} should be
 * used instead.
 *
 * @author Steve Ebersole
 */
public interface QualifiedName {
	Identifier getCatalogName();
	Identifier getSchemaName();
	Identifier getObjectName();

	/**
	 * Returns a string form of the qualified name.
	 * <p>
	 * Depending on intention, may not be appropriate.
	 * May want {@link SqlStringGenerationContext#format}
	 * instead.
	 *
	 * @return The string form
	 */
	String render();
}
