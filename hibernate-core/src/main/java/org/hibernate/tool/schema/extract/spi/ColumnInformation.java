package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing table columns
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public interface ColumnInformation extends ColumnTypeInformation {
	/**
	 * Access to the containing table.
	 *
	 * @return The containing table information
	 */
	@Nonnull TableInformation getContainingTableInformation();

	/**
	 * The simple (not qualified) column name.
	 *
	 * @return The column simple identifier.
	 */
	@Nonnull Identifier getColumnIdentifier();
}
