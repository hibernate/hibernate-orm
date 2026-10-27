package org.hibernate.tool.schema.extract.spi;

import java.sql.Types;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;

/**
 * Provides access to information about existing table columns
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
@Incubating(since = "6.1")
public interface ColumnTypeInformation {

	@Nonnull
	ColumnTypeInformation EMPTY = new ColumnTypeInformation() {
		@Override
		public @Nullable Boolean getNullable() {
			return null;
		}

		@Override
		public int getTypeCode() {
			return Types.OTHER;
		}

		@Override
		public @Nullable String getTypeName() {
			return null;
		}

		@Override
		public int getColumnSize() {
			return 0;
		}

		@Override
		public int getDecimalDigits() {
			return 0;
		}
	};

	/**
	 * Is the column nullable?
	 * <p>
	 * The database is allowed to report unknown, hence the use of {@link Boolean}.
	 *
	 * @return nullability, if known
	 */
	@Nullable Boolean getNullable();

	/**
	 * The JDBC type-code.
	 *
	 * @return JDBC type-code
	 */
	int getTypeCode();

	/**
	 * The database specific type name.
	 *
	 * @return Type name
	 */
	@Nullable String getTypeName();

	// todo : wrap these in org.hibernate.metamodel.spi.relational.Size

	/**
	 * The column size (length).
	 *
	 * @return The column length
	 */
	int getColumnSize();

	/**
	 * The precision, for numeric types
	 *
	 * @return The numeric precision
	 */
	int getDecimalDigits();
}
