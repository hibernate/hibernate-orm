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
package org.hibernate.metamodel.source.annotations.attribute;

import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.binder.ColumnSource;

/**
 * @author Steve Ebersole
 */
public class ColumnValuesSourceImpl implements ColumnSource {
	private ColumnValues columnValues;

	public ColumnValuesSourceImpl(ColumnValues columnValues) {
		this.columnValues = columnValues;
	}

	void setOverrideColumnValues(ColumnValues columnValues) {
		this.columnValues = columnValues;
	}

	@Override
	public String getName() {
		return columnValues.getName();
	}

	@Override
	public boolean isNullable() {
		return columnValues.isNullable();
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public String getSqlType() {
		// todo
		return null;
	}

	@Override
	public Datatype getDatatype() {
		// todo
		return null;
	}

	@Override
	public Size getSize() {
		return new Size(
				columnValues.getPrecision(),
				columnValues.getScale(),
				columnValues.getLength(),
				Size.LobMultiplier.NONE
		);
	}

	@Override
	public boolean isUnique() {
		return columnValues.isUnique();
	}

	@Override
	public String getComment() {
		// todo
		return null;
	}

	@Override
	public boolean isIncludedInInsert() {
		return columnValues.isInsertable();
	}

	@Override
	public boolean isIncludedInUpdate() {
		return columnValues.isUpdatable();
	}

	@Override
	public String getContainingTableName() {
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
