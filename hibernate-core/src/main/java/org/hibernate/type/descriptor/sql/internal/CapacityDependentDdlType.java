/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.dialect.Dialect;

/**
 * Descriptor for a SQL type.
 *
 * @author Christian Beikov
 */
public class CapacityDependentDdlType extends DdlTypeImpl {

	private final TypeEntry[] typeEntries;

	private CapacityDependentDdlType(Builder builder) {
		super(
				builder.sqlTypeCode,
				builder.typeNamePattern,
				builder.castTypeNamePattern,
				builder.castTypeName,
				builder.dialect
		);
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

	public static Builder builder(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		return builder( sqlTypeCode, typeNamePattern, typeNamePattern, dialect );
	}

	public static Builder builder(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return new Builder( sqlTypeCode, typeNamePattern, null, castTypeName, dialect );
	}

	public static Builder builder(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return new Builder( sqlTypeCode, typeNamePattern, castTypeNamePattern, castTypeName, dialect );
	}

	public static class Builder {
		private final int sqlTypeCode;
		private final String typeNamePattern;
		private final String castTypeNamePattern;
		private final String castTypeName;
		private final Dialect dialect;
		private final List<TypeEntry> typeEntries;

		private Builder(
				int sqlTypeCode,
				String typeNamePattern,
				String castTypeNamePattern,
				String castTypeName,
				Dialect dialect) {
			this.sqlTypeCode = sqlTypeCode;
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
}
