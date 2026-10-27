package org.hibernate.tool.schema.extract.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * JDBC column metadata
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public class ColumnInformationImpl implements ColumnInformation {
	private final TableInformation containingTableInformation;
	private final Identifier columnIdentifier;

	private final int typeCode;
	private final String typeName;
	private final int columnSize;
	private final int decimalDigits;
	private final Boolean nullable;

	public ColumnInformationImpl(
			TableInformation containingTableInformation,
			Identifier columnIdentifier,
			int typeCode,
			String typeName,
			int columnSize,
			int decimalDigits,
			Boolean nullable) {
		this.containingTableInformation = containingTableInformation;
		this.columnIdentifier = columnIdentifier;
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.columnSize = columnSize;
		this.decimalDigits = decimalDigits;
		this.nullable = nullable;
	}

	@Override
	public @Nonnull TableInformation getContainingTableInformation() {
		return containingTableInformation;
	}

	@Override
	public @Nonnull Identifier getColumnIdentifier() {
		return columnIdentifier;
	}

	@Override
	public int getTypeCode() {
		return typeCode;
	}

	@Override
	public @Nullable String getTypeName() {
		return typeName;
	}

	@Override
	public int getColumnSize() {
		return columnSize;
	}

	@Override
	public int getDecimalDigits() {
		return decimalDigits;
	}

	@Override
	public @Nullable Boolean getNullable() {
		return nullable;
	}

	public String toString() {
		return "ColumnInformation(" + columnIdentifier + ')';
	}
}
