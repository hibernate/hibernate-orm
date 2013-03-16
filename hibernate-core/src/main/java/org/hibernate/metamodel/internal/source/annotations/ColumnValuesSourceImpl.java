/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.SizeSource;

/**
 * @author Steve Ebersole
 */
public class ColumnValuesSourceImpl implements ColumnSource {
	private Column columnValues;

	public ColumnValuesSourceImpl(Column columnValues) {
		this.columnValues = columnValues;
	}

	void setOverrideColumnValues(Column columnValues) {
		this.columnValues = columnValues;
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
	}

	@Override
	public String getName() {
		return columnValues == null ? null : columnValues.getName();
	}

	@Override
	public TruthValue isNullable() {
		if(columnValues == null) return null;
		return columnValues.isNullable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public String getSqlType() {
		if(columnValues == null) return null;
		return columnValues.getColumnDefinition();
	}

	@Override
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		if(columnValues == null) return null;
		return new SizeSourceImpl(
				columnValues.getPrecision(), columnValues.getScale(), columnValues.getLength()
		);
	}

	@Override
	public boolean isUnique() {
		return columnValues != null && columnValues.isUnique();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public TruthValue isIncludedInInsert() {
		if(columnValues == null) return null;
		return columnValues.isInsertable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public TruthValue isIncludedInUpdate() {
		if(columnValues == null) return null;
		return columnValues.isUpdatable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public String getContainingTableName() {
		if(columnValues == null) return null;
		return columnValues.getTable();
	}


	// these come from attribute ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getReadFragment() {
		return null;
	}

	@Override
	public String getWriteFragment() {
		return null;
	}

	@Override
	public String getCheckCondition() {
		return null;
	}
}
