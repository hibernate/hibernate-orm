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

import org.hibernate.internal.jaxb.mapping.hbm.JaxbColumnElement;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.binder.ColumnSource;

/**
* @author Steve Ebersole
*/
class ColumnSourceImpl implements ColumnSource {
	private final String tableName;
	private final JaxbColumnElement columnElement;
	private boolean includedInInsert;
	private boolean includedInUpdate;
    private final boolean isForceNotNull;

	ColumnSourceImpl(
			String tableName,
			JaxbColumnElement columnElement,
			boolean isIncludedInInsert,
			boolean isIncludedInUpdate) {
		this(tableName, columnElement, isIncludedInInsert, isIncludedInUpdate, false);
	}
    ColumnSourceImpl(
            String tableName,
            JaxbColumnElement columnElement,
            boolean isIncludedInInsert,
            boolean isIncludedInUpdate,
            boolean isForceNotNull) {
        this.tableName = tableName;
        this.columnElement = columnElement;
        this.isForceNotNull = isForceNotNull;
        includedInInsert = isIncludedInInsert;
        includedInUpdate = isIncludedInUpdate;
    }

	@Override
	public String getName() {
		return columnElement.getName();
	}

	@Override
	public boolean isNullable() {
        if(isForceNotNull)return false;
		return ! columnElement.isNotNull();
	}

	@Override
	public String getDefaultValue() {
		return columnElement.getDefault();
	}

	@Override
	public String getSqlType() {
		return columnElement.getSqlType();
	}

	@Override
	public Datatype getDatatype() {
		return null;
	}

	@Override
	public Size getSize() {
		return new Size(
				Helper.getIntValue( columnElement.getPrecision(), -1 ),
				Helper.getIntValue( columnElement.getScale(), -1 ),
				Helper.getLongValue( columnElement.getLength(), -1 ),
				Size.LobMultiplier.NONE
		);
	}

	@Override
	public String getReadFragment() {
		return columnElement.getRead();
	}

	@Override
	public String getWriteFragment() {
		return columnElement.getWrite();
	}

	@Override
	public boolean isUnique() {
		return columnElement.isUnique();
	}

	@Override
	public String getCheckCondition() {
		return columnElement.getCheck();
	}

	@Override
	public String getComment() {
		return columnElement.getComment();
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
}
