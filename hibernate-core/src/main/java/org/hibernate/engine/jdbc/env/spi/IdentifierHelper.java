/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	Identifier normalizeQuoting(Identifier identifier);

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
	Identifier toIdentifier(String text);

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
	Identifier toIdentifier(String text, boolean quoted);

	/**
	 * Needed to account for certain fields ({@link javax.persistence.Column#columnDefinition()} comes to mind)
	 * that need to be quoted id global identifier quoting is requested, but only for spec compliance.  TBH, I can
	 * not think of a argument why column-definitions should ever be *globally* quoted, but the spec is the spec.
	 *
	 * @param text The text to be (possibly) quoted
	 *
	 * @return The identifier form
	 */
	Identifier applyGlobalQuoting(String text);

	/**
	 * Check whether the given word represents a reserved word.
	 *
	 * @param word The word to check
	 *
	 * @return {@code true} if the given word represents a reserved word; {@code false} otherwise.
	 */
	boolean isReservedWord(String word);

	/**
	 * Render the Identifier representation of a catalog name into the String form needed
	 * in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param catalogIdentifier The Identifier representation of a catalog name
	 *
	 * @return The String representation of the given catalog name
	 */
	String toMetaDataCatalogName(Identifier catalogIdentifier);

	/**
	 * Render the Identifier representation of a schema name into the String form needed
	 * in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param schemaIdentifier The Identifier representation of a schema name
	 *
	 * @return The String representation of the given schema name
	 */
	String toMetaDataSchemaName(Identifier schemaIdentifier);

	/**
	 * Render the Identifier representation of an object name (table, sequence, etc) into the
	 * String form needed in {@link java.sql.DatabaseMetaData} calls.
	 *
	 * @param identifier The Identifier representation of an object name
	 *
	 * @return The String representation of the given object name
	 */
	String toMetaDataObjectName(Identifier identifier);
}
