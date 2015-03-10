/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Helper for handling {@link Identifier} instances.
 *
 * @author Steve Ebersole
 */
public interface IdentifierHelper {
	/**
	 * Essentially quotes the identifier if it needs to be.  Useful to apply global quoting,
	 * as well as reserved word quoting after calls to naming strategies.
	 *
	 * @param identifier The identifier for which to normalize quoting.
	 *
	 * @return The quoting-normalized Identifier.
	 */
	public Identifier normalizeQuoting(Identifier identifier);

	/**
	 * Generate an Identifier instance from its simple name as obtained from mapping
	 * information.
	 * <p/>
	 * Note that Identifiers returned from here may be implicitly quoted based on
	 * 'globally quoted identifiers' or based on reserved words.
	 *
	 * @param text The text form of a name as obtained from mapping information.
	 *
	 * @return The identifier form of the name.
	 */
	public Identifier toIdentifier(String text);

	/**
	 * Generate an Identifier instance from its simple name as obtained from mapping
	 * information.  Additionally, this form takes a boolean indicating whether to
	 * explicitly quote the Identifier.
	 * <p/>
	 * Note that Identifiers returned from here may be implicitly quoted based on
	 * 'globally quoted identifiers' or based on reserved words.
	 *
	 * @param text The text form of a name as obtained from mapping information.
	 * @param quoted Is the identifier to be quoted explicitly.
	 *
	 * @return The identifier form of the name.
	 */
	public Identifier toIdentifier(String text, boolean quoted);

	/**
	 * Render the Identifier representation of a catalog name into the String form needed
	 * in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param catalogIdentifier The Identifier representation of a catalog name
	 *
	 * @return The String representation of the given catalog name
	 */
	public String toMetaDataCatalogName(Identifier catalogIdentifier);

	/**
	 * Render the Identifier representation of a schema name into the String form needed
	 * in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param schemaIdentifier The Identifier representation of a schema name
	 *
	 * @return The String representation of the given schema name
	 */
	public String toMetaDataSchemaName(Identifier schemaIdentifier);

	/**
	 * Render the Identifier representation of an object name (table, sequence, etc) into the
	 * String form needed in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param identifier The Identifier representation of an object name
	 *
	 * @return The String representation of the given object name
	 */
	public String toMetaDataObjectName(Identifier identifier);

	/**
	 * Parse an Identifier representation from the String representation of a catalog name
	 * as obtained from {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param catalogName The String representation of a catalog name
	 *
	 * @return The parsed Identifier representation of the given catalog name
	 */
	public Identifier fromMetaDataCatalogName(String catalogName);

	/**
	 * Parse an Identifier representation from the String representation of a schema name
	 * as obtained from {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param schemaName The String representation of a schema name
	 *
	 * @return The parsed Identifier representation of the given schema name
	 */
	public Identifier fromMetaDataSchemaName(String schemaName);

	/**
	 * Parse an Identifier representation from the String representation of an object name
	 * as obtained from {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param name The String representation of an object name
	 *
	 * @return The parsed Identifier representation of the given object name
	 */
	public Identifier fromMetaDataObjectName(String name);
}
