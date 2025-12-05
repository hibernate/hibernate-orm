/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.io.Serializable;

import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.collections.ArrayHelper.indexOf;

public final class CachedJdbcValuesMetadata implements JdbcValuesMetadata, Serializable {
	private final String[] columnNames;
	private final BasicType<?>[] types;

	public CachedJdbcValuesMetadata(String[] columnNames, BasicType<?>[] types) {
		this.columnNames = columnNames;
		this.types = types;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		final int position = indexOf( columnNames, columnName ) + 1;
		if ( position == 0 ) {
			throw new IllegalStateException( "Unexpected resolving of unavailable column: " + columnName );
		}
		return position;
	}

	@Override
	public String resolveColumnName(int position) {
		final String name = columnNames[position - 1];
		if ( name == null ) {
			throw new IllegalStateException( "Unexpected resolving of unavailable column at position: " + position );
		}
		return name;
	}

	@Override
	public <J> BasicType<J> resolveType(
			int position,
			JavaType<J> explicitJavaType,
			TypeConfiguration typeConfiguration) {
		final var type = types[position - 1];
		if ( type == null ) {
			throw new IllegalStateException( "Unexpected resolving of unavailable column at position: " + position );
		}
		if ( explicitJavaType == null || type.getJavaTypeDescriptor() == explicitJavaType ) {
			//noinspection unchecked
			return (BasicType<J>) type;
		}
		else {
			return typeConfiguration.getBasicTypeRegistry()
					.resolve( explicitJavaType, type.getJdbcType() );
		}
	}

}
