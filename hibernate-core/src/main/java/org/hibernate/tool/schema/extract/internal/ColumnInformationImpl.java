/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.spi.relational.Identifier;
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






