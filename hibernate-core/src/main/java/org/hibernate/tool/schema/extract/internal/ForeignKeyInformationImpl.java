package org.hibernate.tool.schema.extract.internal;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;

/**
 * @author Steve Ebersole
 */
public class ForeignKeyInformationImpl implements ForeignKeyInformation {
	private final Identifier foreignKeyIdentifier;
	private final List<ColumnReferenceMapping> columnMappingList;

	public ForeignKeyInformationImpl(
			Identifier foreignKeyIdentifier,
			List<ColumnReferenceMapping> columnMappingList) {
		this.foreignKeyIdentifier = foreignKeyIdentifier;
		this.columnMappingList = columnMappingList;
	}

	@Override
	public @Nonnull Identifier getForeignKeyIdentifier() {
		return foreignKeyIdentifier;
	}

	@Override
	public @Nonnull Iterable<ColumnReferenceMapping> getColumnReferenceMappings() {
		return columnMappingList;
	}

	public record ColumnReferenceMappingImpl(ColumnInformation referencing, ColumnInformation referenced)
			implements ColumnReferenceMapping {
		@Override
		public @Nonnull ColumnInformation getReferencingColumnMetadata() {
			return referencing;
		}

		@Override
		public @Nonnull ColumnInformation getReferencedColumnMetadata() {
			return referenced;
		}
	}
}
