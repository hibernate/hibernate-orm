/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.IdentifierSupport;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Handles configured database identifiers at runtime.
///
/// A Dialect provider uses this helper after Hibernate has applied identifier
/// casing, keyword, quoting, and namespace configuration. Configure the
/// boot-scoped builder received by
/// [IdentifierSupport#buildIdentifierHelper(IdentifierHelperBuildRequest)]
/// before invoking the inherited build operation. When builder configuration
/// cannot express a database rule, decorate the resulting helper with
/// [org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper]. Do not
/// implement this interface directly as a provider contract.
///
/// @author Steve Ebersole
/// @see IdentifierSupport#buildIdentifierHelper(IdentifierHelperBuildRequest)
@SPI({ USE, SUPPLY })
public interface IdentifierHelper {
	/// Quote the identifier when required by the configured global, reserved-word,
	/// or database-specific policy.
	///
	/// @param identifier the identifier to normalize
	/// @return the quoting-normalized identifier
	Identifier normalizeQuoting(Identifier identifier);

	/// Create an identifier from mapping text, applying configured implicit
	/// quoting when necessary.
	///
	/// @param text the name obtained from mapping information
	/// @return the identifier form of the name
	Identifier toIdentifier(String text);

	/// Create an identifier from mapping text, honoring explicit quoting and then
	/// applying configured implicit quoting when necessary.
	///
	/// @param text the name obtained from mapping information
	/// @param quoted whether the identifier is explicitly quoted
	/// @return the identifier form of the name
	Identifier toIdentifier(String text, boolean quoted);

	/// Create an identifier from mapping text while retaining whether the mapping
	/// supplied the name explicitly.
	///
	/// @param text the name obtained from mapping information
	/// @param quoted whether the identifier is explicitly quoted
	/// @param isExplicit whether the mapping supplied the name explicitly
	/// @return the identifier form of the name
	Identifier toIdentifier(String text, boolean quoted, boolean isExplicit);

	/// Apply global quoting to a `column-definition` fragment as required by
	/// Jakarta Persistence. Do not use this operation for ordinary identifiers.
	///
	/// Applications may exclude column definitions from global quoting through
	/// [org.hibernate.cfg.MappingSettings#GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS].
	///
	/// @param text the text to quote when global policy requires it
	/// @return the identifier form
	Identifier applyGlobalQuoting(String text);

	/// Determine whether the configured environment treats a word as reserved.
	///
	/// @param word the word to check
	/// @return `true` when the word is reserved
	boolean isReservedWord(String word);

	/// Render a catalog identifier for `java.sql.DatabaseMetaData` calls.
	///
	/// @param catalogIdentifier the catalog identifier, or `null` for the
	/// configured current-catalog behavior
	/// @return the JDBC metadata catalog name
	String toMetaDataCatalogName(Identifier catalogIdentifier);

	/// Render a schema identifier for `java.sql.DatabaseMetaData` calls.
	///
	/// @param schemaIdentifier the schema identifier, or `null` for the
	/// configured current-schema behavior
	/// @return the JDBC metadata schema name
	String toMetaDataSchemaName(Identifier schemaIdentifier);

	/// Render a required database-object identifier for
	/// `java.sql.DatabaseMetaData` calls.
	///
	/// @param identifier the required object identifier
	/// @return the JDBC metadata object name
	String toMetaDataObjectName(Identifier identifier);
}
