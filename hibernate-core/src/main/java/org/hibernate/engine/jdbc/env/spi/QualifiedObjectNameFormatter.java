package org.hibernate.engine.jdbc.env.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;

/**
 * Contract for rendering qualified object names for use in queries, etc.
 *
 * @author Steve Ebersole
 */
public interface QualifiedObjectNameFormatter {
	/**
	 * Render a formatted a table name
	 *
	 * @param qualifiedTableName The table name
	 * @param dialect The dialect
	 *
	 * @return The formatted name,
	 */
	@Nonnull
	String format(@Nonnull QualifiedTableName qualifiedTableName, @Nonnull Dialect dialect);

	/**
	 * Render a formatted sequence name
	 *
	 * @param qualifiedSequenceName The sequence name
	 * @param dialect The dialect
	 *
	 * @return The formatted name
	 */
	@Nonnull
	String format(@Nonnull QualifiedSequenceName qualifiedSequenceName, @Nonnull Dialect dialect);

	/**
	 * Render a formatted non-table and non-sequence qualified name
	 *
	 * @param qualifiedName The name
	 * @param dialect The dialect
	 *
	 * @return The formatted name
	 */
	String format(QualifiedName qualifiedName, Dialect dialect);
	/** Render finalized physical components using database qualifier ordering. */
	String format(QualifiedPhysicalName name, Dialect dialect);
}
