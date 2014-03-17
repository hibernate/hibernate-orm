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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.spi.relational.JdbcDataType;

/**
 * @author Hardy Ferentschik
 */
public class ColumnSourceImpl implements ColumnSource {
	private final Column columnValues;
	private final String defaultTableName;
	private final String readFragment;
	private final String writeFragment;
	private final String checkCondition;

	public ColumnSourceImpl(Column columnValues) {
		this( columnValues, null );
	}

	public ColumnSourceImpl(Column columnValues, String defaultTableName) {
		this( columnValues, defaultTableName, null, null, null );
	}

	public ColumnSourceImpl(
			Column columnValues,
			String defaultTableName,
			String readFragment,
			String writeFragment,
			String checkCondition) {
		this.columnValues = columnValues;
		this.defaultTableName = defaultTableName;
		this.readFragment = readFragment;
		this.writeFragment = writeFragment;
		this.checkCondition = checkCondition;
	}

	public ColumnSourceImpl(AbstractPersistentAttribute attribute, Column columnValues) {
		this( attribute, columnValues, null );
	}

	public ColumnSourceImpl(AbstractPersistentAttribute attribute, Column columnValues, String defaultTableName) {
		boolean isBasicAttribute = attribute != null && attribute.getNature() == AbstractPersistentAttribute.Nature.BASIC;
		this.readFragment = attribute != null && isBasicAttribute ? ( (BasicAttribute) attribute ).getCustomReadFragment() : null;
		this.writeFragment = attribute != null && isBasicAttribute ? ( (BasicAttribute) attribute ).getCustomWriteFragment() : null;
		this.checkCondition = attribute != null ? attribute.getCheckCondition() : null;
		this.columnValues = columnValues;
		this.defaultTableName = defaultTableName;
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
		if ( columnValues == null || columnValues.isNullable() == null ) {
			return null;
		}
		return columnValues.isNullable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public String getSqlType() {
		if ( columnValues == null ) {
			return null;
		}
		return columnValues.getColumnDefinition();
	}

	@Override
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		if ( columnValues == null ) {
			return null;
		}
		return new SizeSourceImpl(
				columnValues.getPrecision(), columnValues.getScale(), columnValues.getLength()
		);
	}

	@Override
	public boolean isUnique() {
		return columnValues != null && columnValues.isUnique() != null && columnValues.isUnique();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public TruthValue isIncludedInInsert() {
		if ( columnValues == null || columnValues.isInsertable() == null) {
			return null;
		}
		return columnValues.isInsertable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public TruthValue isIncludedInUpdate() {
		if ( columnValues == null || columnValues.isUpdatable() == null) {
			return null;
		}
		return columnValues.isUpdatable() ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public String getContainingTableName() {
		if ( columnValues == null ) {
			return defaultTableName;
		}
		else if ( columnValues.getTable() == null ) {
			return defaultTableName;
		}
		else {
			return columnValues.getTable();
		}
	}

	// these come from attribute ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getReadFragment() {
		return readFragment;
	}

	@Override
	public String getWriteFragment() {
		return writeFragment;
	}

	@Override
	public String getCheckCondition() {
		return checkCondition;
	}
}


