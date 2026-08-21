/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for a SQL type.
 *
 * @author Christian Beikov
 */
public class CapacityDependentDdlType extends DdlTypeImpl {

	private final LobKind lobKind;
	private final TypeEntry[] typeEntries;

	private CapacityDependentDdlType(Builder builder) {
		super(
				builder.sqlTypeCode,
				builder.typeNamePattern,
				builder.castTypeNamePattern,
				builder.castTypeName,
				builder.dialect
		);
		this.lobKind = builder.lobKind;
		builder.typeEntries.sort( Comparator.naturalOrder() );
		this.typeEntries = builder.typeEntries.toArray(new TypeEntry[0]);
	}

	@Override @Deprecated
	public String[] getRawTypeNames() {
		final String[] rawTypeNames = new String[typeEntries.length + 1];
		for ( int i = 0; i < typeEntries.length; i++ ) {
			//trim off the length/precision/scale
			final String typeNamePattern = typeEntries[i].typeNamePattern;
			final int paren = typeNamePattern.indexOf( '(' );
			if ( paren > 0 ) {
				final int parenEnd = typeNamePattern.lastIndexOf( ')' );
				rawTypeNames[i] = parenEnd + 1 == typeNamePattern.length()
						? typeNamePattern.substring( 0, paren )
						: ( typeNamePattern.substring( 0, paren ) + typeNamePattern.substring( parenEnd + 1 ) );
			}
			else {
				rawTypeNames[i] = typeNamePattern;
			}
		}
		rawTypeNames[typeEntries.length] = getRawTypeName();
		return rawTypeNames;
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		if ( size != null && size > 0 ) {
			for ( TypeEntry typeEntry : typeEntries ) {
				if ( size <= typeEntry.capacity ) {
					return replace( typeEntry.typeNamePattern, size, precision, scale );
				}
			}
		}
		else if ( precision != null && precision > 0 ) {
			for ( TypeEntry typeEntry : typeEntries ) {
				if ( precision <= typeEntry.capacity ) {
					return replace( typeEntry.typeNamePattern, size, precision, scale );
				}
			}
		}
		return super.getTypeName( size, precision, scale );
	}

	@Override
	public boolean isLob(Size size) {
		if ( lobKind == LobKind.ALL_LOB ) {
			return true;
		}
		final Long length = size.getLength();
		if ( length != null && length > 0 ) {
			for ( TypeEntry typeEntry : typeEntries ) {
				if ( length <= typeEntry.capacity ) {
					return false;
				}
			}
		}
		return lobKind == LobKind.BIGGEST_LOB;
	}

	public static Builder builder(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		return builder(
				sqlTypeCode,
				JdbcType.isLob( sqlTypeCode ) ? LobKind.ALL_LOB : LobKind.NONE,
				typeNamePattern,
				typeNamePattern,
				dialect
		);
	}

	public static Builder builder(int sqlTypeCode, LobKind lobKind, String typeNamePattern, Dialect dialect) {
		return builder( sqlTypeCode, lobKind, typeNamePattern, typeNamePattern, dialect );
	}

	public static Builder builder(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return builder(
				sqlTypeCode,
				JdbcType.isLob( sqlTypeCode ) ? LobKind.ALL_LOB : LobKind.NONE,
				typeNamePattern,
				castTypeName,
				dialect
		);
	}

	public static Builder builder(
			int sqlTypeCode,
			LobKind lobKind,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return builder( sqlTypeCode, lobKind, typeNamePattern, null, castTypeName, dialect );
	}

	public static Builder builder(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return builder(
				sqlTypeCode,
				JdbcType.isLob( sqlTypeCode ) ? LobKind.ALL_LOB : LobKind.NONE,
				typeNamePattern,
				castTypeNamePattern,
				castTypeName,
				dialect
		);
	}

	public static Builder builder(
			int sqlTypeCode,
			LobKind lobKind,
			String typeNamePattern,
			String castTypeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return new Builder( sqlTypeCode, lobKind, typeNamePattern, castTypeNamePattern, castTypeName, dialect );
	}

	public static class Builder {
		private final int sqlTypeCode;
		private final LobKind lobKind;
		private final String typeNamePattern;
		private final String castTypeNamePattern;
		private final String castTypeName;
		private final Dialect dialect;
		private final List<TypeEntry> typeEntries;

		private Builder(
				int sqlTypeCode,
				LobKind lobKind,
				String typeNamePattern,
				String castTypeNamePattern,
				String castTypeName,
				Dialect dialect) {
			this.sqlTypeCode = sqlTypeCode;
			this.lobKind = lobKind;
			this.typeNamePattern = typeNamePattern;
			this.castTypeNamePattern = castTypeNamePattern;
			this.castTypeName = castTypeName;
			this.dialect = dialect;
			this.typeEntries = new ArrayList<>();
		}

		public Builder withTypeCapacity(long capacity, String typeNamePattern) {
			typeEntries.add( new TypeEntry( capacity, typeNamePattern ) );
			return this;
		}

		public CapacityDependentDdlType build() {
			return new CapacityDependentDdlType( this );
		}
	}

	private static class TypeEntry implements Comparable<TypeEntry> {
		private final long capacity;
		private final String typeNamePattern;

		public TypeEntry(long capacity, String typeNamePattern) {
			this.capacity = capacity;
			this.typeNamePattern = typeNamePattern;
		}

		@Override
		public int compareTo(TypeEntry o) {
			return Long.compare( capacity, o.capacity );
		}
	}

	public enum LobKind {
		BIGGEST_LOB,
		ALL_LOB,
		NONE
	}
}
