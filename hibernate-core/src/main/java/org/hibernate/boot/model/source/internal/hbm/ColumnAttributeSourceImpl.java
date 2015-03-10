/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.JdbcDataType;
import org.hibernate.boot.model.source.spi.SizeSource;

/**
 * Implementation of a {@link ColumnSource} when the column is declared as just the name via the column XML
 * attribute.  For example, {@code <property name="socialSecurityNumber" column="ssn"/>}.
 *
 * @author Steve Ebersole
 */
class ColumnAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements ColumnSource {

	private final String tableName;
	private final String columnName;
	private final SizeSource sizeSource;
    private final TruthValue nullable;
    private final TruthValue unique;

    ColumnAttributeSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			String columnName,
			SizeSource sizeSource,
            TruthValue nullable,
			TruthValue unique) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnName = columnName;
		this.sizeSource = sizeSource;
        this.nullable = nullable;
		this.unique = unique;
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
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
	public TruthValue isNullable() {
		return nullable;
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
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		return sizeSource;
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
		return unique == TruthValue.TRUE;
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
