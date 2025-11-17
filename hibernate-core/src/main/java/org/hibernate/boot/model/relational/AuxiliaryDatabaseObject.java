/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * An auxiliary database object is a trigger, function, stored procedure,
 * or anything similar which is defined using explicit handwritten DDL
 * {@code create} and {@code drop} statements in the mapping metadata.
 * <p>
 * This SPI allows Hibernate to export and clean up these objects along
 * with the rest of the schema.
 *
 * @author Steve Ebersole
 */
public interface AuxiliaryDatabaseObject extends Exportable, Serializable {
	/**
	 * Does this database object apply to the given dialect?
	 *
	 * @param dialect The dialect to check against.
	 * @return True if this database object does apply to the given dialect.
	 */
	boolean appliesToDialect(Dialect dialect);

	/**
	 * Defines a simple precedence.
	 * Should creation of this auxiliary object happen before creation of tables?
	 * <ul>
	 * <li>If {@code true}, the auxiliary object creation will happen after any
	 *     explicit schema creation but before creation of tables and sequences.
	 * <li>If {@code false}, the auxiliary object creation will happen after
	 *     explicit schema creation and after creation of tables and sequences.
	 * </ul>
	 * <p>
	 * This precedence is automatically inverted for when the schema is dropped.
	 *
	 * @return {@code true} indicates this object should be created before tables;
	 *         {@code false} indicates it should be created after tables.
	 */
	boolean beforeTablesOnCreation();

	/**
	 * Gets the SQL strings for creating the database object.
	 *
	 * @param context A context to help generate the SQL creation strings
	 *
	 * @return the SQL strings for creating the database object.
	 */
	String[] sqlCreateStrings(SqlStringGenerationContext context);

	/**
	 * Gets the SQL strings for dropping the database object.
	 *
	 * @param context A context to help generate the SQL drop strings
	 *
	 * @return the SQL strings for dropping the database object.
	 */
	String[] sqlDropStrings(SqlStringGenerationContext context);

	/**
	 * Additional, optional interface for {@code AuxiliaryDatabaseObject}s
	 * that want to allow expansion of allowable dialects via mapping.
	 */
	interface Expandable {
		void addDialectScope(String dialectName);
	}
}
