/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;


import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Defines a contract for exporting of database objects (tables, sequences, etc)
 * for use in SQL {@code CREATE} and {@code DROP} scripts.
 *
 * @apiNote This is an ORM-centric contract.
 *
 * @author Steve Ebersole
 */
public interface Exporter<T extends Exportable> {
	String[] NO_COMMANDS = ArrayHelper.EMPTY_STRING_ARRAY;

	/**
	 * Get the commands needed for creation.
	 *
	 * @return The commands needed for creation scripting.
	 */
	String[] getSqlCreateStrings(T exportable, Metadata metadata, SqlStringGenerationContext context);

	/**
	 * Get the commands needed for dropping.
	 *
	 * @return The commands needed for drop scripting.
	 */
	String[] getSqlDropStrings(T exportable, Metadata metadata, SqlStringGenerationContext context);
}
