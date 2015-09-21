/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.boot.model.TruthValue;
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
	private final TruthValue nullable;

	public ColumnInformationImpl(
			TableInformation containingTableInformation,
			Identifier columnIdentifier,
			int typeCode,
			String typeName,
			int columnSize,
			int decimalDigits,
			TruthValue nullable) {
		this.containingTableInformation = containingTableInformation;
		this.columnIdentifier = columnIdentifier;
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.columnSize = columnSize;
		this.decimalDigits = decimalDigits;
		this.nullable = nullable;
	}

	@Override
	public TableInformation getContainingTableInformation() {
		return containingTableInformation;
	}

	@Override
	public Identifier getColumnIdentifier() {
		return columnIdentifier;
	}

	@Override
	public int getTypeCode() {
		return typeCode;
	}

	@Override
	public String getTypeName() {
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
	public TruthValue getNullable() {
		return nullable;
	}

	public String toString() {
		return "ColumnInformation(" + columnIdentifier + ')';
	}
}
