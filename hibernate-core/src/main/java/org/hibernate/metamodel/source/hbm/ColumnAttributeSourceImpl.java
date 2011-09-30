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
package org.hibernate.metamodel.source.hbm;

import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.binder.ColumnSource;

/**
* @author Steve Ebersole
*/
class ColumnAttributeSourceImpl implements ColumnSource {
	private final String tableName;
	private final String columnName;
	private boolean includedInInsert;
	private boolean includedInUpdate;
    private boolean isForceNotNull;

	ColumnAttributeSourceImpl(
			String tableName,
			String columnName,
			boolean includedInInsert,
			boolean includedInUpdate) {
		this(tableName, columnName, includedInInsert, includedInUpdate, false);
	}

    ColumnAttributeSourceImpl(
			String tableName,
			String columnName,
			boolean includedInInsert,
			boolean includedInUpdate,
            boolean isForceNotNull) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.includedInInsert = includedInInsert;
		this.includedInUpdate = includedInUpdate;
        this.isForceNotNull = isForceNotNull;
	}

	@Override
	public boolean isIncludedInInsert() {
		return includedInInsert;
	}

	@Override
	public boolean isIncludedInUpdate() {
		return includedInUpdate;
	}

	@Override
	public String getContainingTableName() {
		return tableName;
	}

	@Override
	public String getName() {
		return columnName;
	}

	@Override
	public boolean isNullable() {
		return !isForceNotNull;
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public String getSqlType() {
		return null;
	}

	@Override
	public Datatype getDatatype() {
		return null;
	}

	@Override
	public Size getSize() {
		return null;
	}

	@Override
	public String getReadFragment() {
		return null;
	}

	@Override
	public String getWriteFragment() {
		return null;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public String getCheckCondition() {
		return null;
	}

	@Override
	public String getComment() {
		return null;
	}
}
