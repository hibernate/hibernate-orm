/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

/**
 * An {@link IdentifierGenerator} that requires creation of database objects.
 * <p>
 * All instances have access to a special mapping parameter in their
 * {@link #configure( GeneratorCreationContext, Properties )} method: schema
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see IdentifierGenerator
 */
public interface PersistentIdentifierGenerator extends OptimizableGenerator {
	/**
	 * The configuration parameter holding the catalog name
	 */
	String CATALOG = "catalog";

	/**
	 * The configuration parameter holding the schema name
	 */
	String SCHEMA = "schema";

	/**
	 * The configuration parameter holding the table name for the
	 * generated id
	 */
	String TABLE = "target_table";

	/**
	 * The configuration parameter holding the table names for all
	 * tables for which the id must be unique
	 */
	String TABLES = "identity_tables";

	/**
	 * The configuration parameter holding the primary key column
	 * name of the generated id
	 */
	String PK = "target_column";

	/**
	 * The key under which to find the {@link org.hibernate.boot.model.naming.ObjectNameNormalizer} in the config param map.
	 *
	 * @deprecated no longer set, use {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment#getIdentifierHelper}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	String IDENTIFIER_NORMALIZER = "identifier_normalizer";

	/**
	 * The configuration parameter holding the generator options.
	 */
	String OPTIONS = "options";
}
