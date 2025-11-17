/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;


import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.JdbcDataType;
import org.hibernate.boot.model.source.spi.SizeSource;

import static java.util.Collections.addAll;
import static org.hibernate.internal.util.StringHelper.splitAtCommas;

/**
 * @author Steve Ebersole
 */
class ColumnSourceImpl
		extends AbstractHbmSourceNode
		implements ColumnSource {
	private final String tableName;
	private final JaxbHbmColumnType columnElement;
	private final Boolean nullable;
	private final Set<String> indexConstraintNames;
	private final Set<String> ukConstraintNames;

	ColumnSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			JaxbHbmColumnType columnElement,
			Set<String> indexConstraintNames,
			Set<String> ukConstraintNames) {
		this(
				mappingDocument,
				tableName,
				columnElement,
				columnElement.isNotNull() == null
						? null
						: !columnElement.isNotNull(),
				indexConstraintNames,
				ukConstraintNames
		);
	}

	ColumnSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			JaxbHbmColumnType columnElement,
			Boolean nullable,
			Set<String> indexConstraintNames,
			Set<String> ukConstraintNames) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnElement = columnElement;
		this.nullable = nullable;

		this.indexConstraintNames =
				splitAndCombine( indexConstraintNames,
						columnElement.getIndex() );
		this.ukConstraintNames =
				splitAndCombine( ukConstraintNames,
						columnElement.getUniqueKey() );
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
	}

	@Override
	public String getName() {
		return columnElement.getName();
	}

	@Override
	public Boolean isNullable() {
		return nullable;
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
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		return Helper.interpretSizeSource(
				columnElement.getLength(),
				columnElement.getScale(),
				columnElement.getPrecision()
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
		return columnElement.isUnique() != null
			&& columnElement.isUnique();
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
	public String getContainingTableName() {
		return tableName;
	}

	@Override
	public Set<String> getIndexConstraintNames() {
		return indexConstraintNames;
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return ukConstraintNames;
	}

	public static Set<String> splitAndCombine(Set<String> stringSet, String values) {
		if ( values == null || values.isEmpty() ) {
			return stringSet;
		}
		else {
			final HashSet<String> set = new HashSet<>( stringSet );
			addAll( set, splitAtCommas( values ) );
			return set;
		}
	}
}
