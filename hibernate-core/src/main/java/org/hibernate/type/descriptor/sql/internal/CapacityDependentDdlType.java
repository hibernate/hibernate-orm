/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

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

	@Override
	public String[] getRawTypeNames() {
		final var rawTypeNames = new String[typeEntries.length + 1];
		for ( int i = 0; i < typeEntries.length; i++ ) {
			rawTypeNames[i] = getRawTypeName( typeEntries[i].typeNamePattern );
		}
		rawTypeNames[typeEntries.length] = super.getRawTypeNames()[0];
		return rawTypeNames;
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Long size = columnSize.getLength();
		final Integer precision = columnSize.getPrecision();
		final Integer scale = columnSize.getScale();
		if ( size != null && size > 0 ) {
			for ( var typeEntry : typeEntries ) {
				if ( size <= typeEntry.capacity ) {
					return replace( typeEntry.typeNamePattern, size, precision, scale );
				}
			}
		}
		else if ( precision != null && precision > 0 ) {
			for ( var typeEntry : typeEntries ) {
				if ( precision <= typeEntry.capacity ) {
					return replace( typeEntry.typeNamePattern, size, precision, scale );
				}
			}
		}
		return formatTypeName( size, precision, scale );
	}

	@Override
	public boolean isLob(Size size) {
		if ( lobKind == LobKind.ALL_LOB ) {
			return true;
		}
		final Long length = size.getLength();
		if ( length != null && length > 0 ) {
			for ( var typeEntry : typeEntries ) {
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
		return builder( sqlTypeCode, lobKind, typeNamePattern, (String) null, castTypeName, dialect );
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

	public static Builder builder(
			int sqlTypeCode,
			LobKind lobKind,
			String typeNamePattern,
			Function<Integer,String> parameterizedCastTypeName,
			String castTypeName,
			Dialect dialect) {
		return new Builder( sqlTypeCode, lobKind, typeNamePattern, parameterizedCastTypeName, castTypeName, dialect );
	}

	public static class Builder {
		private final int sqlTypeCode;
		private final LobKind lobKind;
		private final String typeNamePattern;
		private final String castTypeNamePattern;
		private final String castTypeName;
		private final Dialect dialect;
		private final List<TypeEntry> typeEntries;
		private final Function<Integer,String> parameterizedCastTypeName;

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
			this.parameterizedCastTypeName = null;
			this.castTypeName = castTypeName;
			this.dialect = dialect;
			this.typeEntries = new ArrayList<>();
		}

		private Builder(
				int sqlTypeCode,
				LobKind lobKind,
				String typeNamePattern,
				Function<Integer,String> parameterizedCastTypeName,
				String castTypeName,
				Dialect dialect) {
			this.sqlTypeCode = sqlTypeCode;
			this.lobKind = lobKind;
			this.typeNamePattern = typeNamePattern;
			this.castTypeNamePattern = null;
			this.parameterizedCastTypeName = parameterizedCastTypeName;
			this.castTypeName = castTypeName;
			this.dialect = dialect;
			this.typeEntries = new ArrayList<>();
		}

		public Builder withTypeCapacity(long capacity, String typeNamePattern) {
			typeEntries.add( new TypeEntry( capacity, typeNamePattern ) );
			return this;
		}

		public CapacityDependentDdlType build() {
			return parameterizedCastTypeName == null
					? new CapacityDependentDdlType( this )
					: new CapacityDependentDdlType( this ) {
						@Override
						public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
							return columnSize.getLength() == null
									? super.getCastTypeName( columnSize, type, ddlTypeRegistry )
									: parameterizedCastTypeName.apply( columnSize.getLength().intValue() );
						}
					};
		}
	}

	private record TypeEntry(long capacity, String typeNamePattern)
			implements Comparable<TypeEntry> {
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
