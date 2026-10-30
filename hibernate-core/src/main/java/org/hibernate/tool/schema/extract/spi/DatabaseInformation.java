package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Teodor Danciu
 * @author Steve Ebersole
 */
@Incubating(since = "5.1")
public interface DatabaseInformation {
	/**
	 * Check to see if the given schema already exists.
	 *
	 * @param schema The schema name
	 *
	 * @return {@code true} indicates a schema with the given name already exists
	 */
	boolean schemaExists(@Nonnull Namespace.Name schema);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param catalogName The name of the catalog which contains the schema which the table belongs to
	 * @param schemaName The name of the schema the table belongs to
	 * @param tableName The table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	@Nullable TableInformation getTableInformation(
			@Nullable Identifier catalogName,
			@Nullable Identifier schemaName,
			@Nonnull Identifier tableName);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param schemaName The name of the schema the table belongs to
	 * @param tableName The table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	@Nullable TableInformation getTableInformation(
			@Nonnull Namespace.Name schemaName,
			@Nonnull Identifier tableName);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param tableName The qualified table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	@Nullable TableInformation getTableInformation(@Nonnull QualifiedTableName tableName);

	/**
	 * Obtain reference to all the {@link TableInformation} for a given {@link Namespace}
	 *
	 * @param namespace The {@link Namespace} which contains the {@link TableInformation}
	 *
	 * @return a {@link NameSpaceTablesInformation}
	 */
	@Nonnull NameSpaceTablesInformation getTablesInformation(@Nonnull Namespace namespace);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param catalogName The name of the catalog which contains the schema which the sequence belongs to
	 * @param schemaName The name of the schema the sequence belongs to
	 * @param sequenceName The sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	@Nullable SequenceInformation getSequenceInformation(
			@Nullable Identifier catalogName,
			@Nullable Identifier schemaName,
			@Nonnull Identifier sequenceName);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param schemaName The name of the schema the table belongs to
	 * @param sequenceName The sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	@Nullable SequenceInformation getSequenceInformation(
			@Nonnull Namespace.Name schemaName,
			@Nonnull Identifier sequenceName);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param sequenceName The qualified sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	@Nullable SequenceInformation getSequenceInformation(@Nonnull QualifiedSequenceName sequenceName);

	/**
	 * Check to see if the given catalog already exists.
	 *
	 * @param catalog The catalog name
	 *
	 * @return {@code true} indicates a catalog with the given name already exists
	 */
	boolean catalogExists(@Nonnull Identifier catalog);

	void cleanup();
}
