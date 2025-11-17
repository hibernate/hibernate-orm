/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Set;

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
	private final Boolean nullable;
	private final Boolean unique;
	private final Set<String> indexConstraintNames;
	private final Set<String> ukConstraintNames;

	ColumnAttributeSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			String columnName,
			SizeSource sizeSource,
			Boolean nullable,
			Boolean unique,
			Set<String> indexConstraintNames,
			Set<String> ukConstraintNames) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnName = columnName;
		this.sizeSource = sizeSource;
		this.nullable = nullable;
		this.unique = unique;
		this.indexConstraintNames = indexConstraintNames;
		this.ukConstraintNames = ukConstraintNames;
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
	public Boolean isNullable() {
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
		return unique == Boolean.TRUE;
	}

	@Override
	public String getCheckCondition() {
		return null;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public Set<String> getIndexConstraintNames() {
		return indexConstraintNames;
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return ukConstraintNames;
	}
}
