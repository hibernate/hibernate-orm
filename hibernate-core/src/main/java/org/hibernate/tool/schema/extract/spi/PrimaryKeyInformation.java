package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing primary key for a table
 *
 * @author Steve Ebersole
 */
public interface PrimaryKeyInformation {
	/**
	 * Obtain the identifier for this PK.
	 *
	 * @return The PK identifier.
	 */
	@Nullable Identifier getPrimaryKeyIdentifier();

	/**
	 * Obtain the columns making up the primary key.  Returned in sequential order.
	 *
	 * @return The columns
	 */
	@Nonnull Iterable<ColumnInformation> getColumns();
}
