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


public final class CachedJdbcValuesMetadata implements JdbcValuesMetadata, Serializable {
	private final String[] columnNames;
	private final BasicType<?>[] types;
	private final int[] valueIndexesToCacheIndexes;

	public CachedJdbcValuesMetadata(String[] columnNames, BasicType<?>[] types, int[] valueIndexesToCacheIndexes) {
		this.columnNames = columnNames;
		this.types = types;
		this.valueIndexesToCacheIndexes = valueIndexesToCacheIndexes;
	}

	public int[] getValueIndexesToCacheIndexes() {
		return valueIndexesToCacheIndexes;
	}

	public JavaType<?> getStoredJavaType(int columnIndex) {
		final var type = types[columnIndex];
		return type != null ? type.getJavaTypeDescriptor() : null;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int resolveColumnPosition(String columnName) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnName.equalsIgnoreCase( columnNames[i] ) ) {
				return i + 1;
			}
		}
		throw new CacheMetadataIncompleteException( "Column unavailable with name: " + columnName );
	}

	@Override
	public String resolveColumnName(int position) {
		final String name = columnNames[position - 1];
		if ( name == null ) {
			throw new CacheMetadataIncompleteException( "Column unavailable at position: " + position );
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
			throw new CacheMetadataIncompleteException( "Column unavailable at position: " + position );
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

	/**
	 * Thrown when the cached metadata does not contain information for a requested column
	 */
	public static class CacheMetadataIncompleteException extends RuntimeException {
		public CacheMetadataIncompleteException(String message) {
			super( message );
		}
	}

}
