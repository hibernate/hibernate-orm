/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

import static org.hibernate.SPI.Role.USE;

/// Builds a standard DDL type descriptor without exposing Hibernate's internal
/// descriptor implementations.
///
/// Start with [StandardDdlTypes#builder(int, String, Dialect)]. For a single
/// SQL type-name pattern, use the defaults or configure its cast names. Add
/// capacity entries when one logical SQL type code selects among multiple
/// physical type names. Each entry is an inclusive upper bound, and the
/// default pattern is used above the largest bound.
///
/// A built descriptor is an immutable snapshot. The builder may be refined and
/// built again without changing descriptors already registered with a
/// [org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry].
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class DdlTypeBuilder {
	/// Determines which sizes represented by a descriptor are LOB values.
	///
	/// @since 8.0
	public enum LobKind {
		/// No represented size is a LOB.
		NONE,
		/// Only the default type above all configured capacity entries is a LOB.
		BIGGEST,
		/// Every represented size is a LOB.
		ALL
	}

	private final int sqlTypeCode;
	private final String typeNamePattern;
	private final Dialect dialect;
	private final Map<Long, String> typeCapacityEntries = new LinkedHashMap<>();

	private LobKind lobKind;
	private String castTypeNamePattern;
	private String castTypeName;
	private String narrowCastTypeName;
	private boolean narrowCastTypeNameSpecified;
	private IntFunction<String> parameterizedCastTypeName;

	DdlTypeBuilder(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		this.sqlTypeCode = sqlTypeCode;
		this.typeNamePattern = requireText( typeNamePattern, "typeNamePattern" );
		this.dialect = requireNonNull( dialect, "dialect" );
		this.lobKind = JdbcType.isLob( sqlTypeCode ) ? LobKind.ALL : LobKind.NONE;
		this.castTypeName = typeNamePattern;
		this.narrowCastTypeName = typeNamePattern;
	}

	/// Classify the sizes represented by the built descriptor as LOB values.
	public DdlTypeBuilder lobKind(LobKind lobKind) {
		this.lobKind = requireNonNull( lobKind, "lobKind" );
		return this;
	}

	/// Use a distinct cast pattern when an explicit size is available.
	///
	/// This setting is mutually exclusive with
	/// [#parameterizedCastTypeName(IntFunction)].
	public DdlTypeBuilder castTypeNamePattern(String pattern) {
		if ( parameterizedCastTypeName != null ) {
			throw new IllegalArgumentException(
					"castTypeNamePattern cannot be combined with parameterizedCastTypeName"
			);
		}
		this.castTypeNamePattern = requireText( pattern, "castTypeNamePattern" );
		return this;
	}

	/// Use this SQL type name for an unsized cast target.
	///
	/// Until [#narrowCastTypeName(String)] is called, the narrow-cast name tracks
	/// this value.
	public DdlTypeBuilder castTypeName(String name) {
		this.castTypeName = requireText( name, "castTypeName" );
		if ( !narrowCastTypeNameSpecified ) {
			narrowCastTypeName = this.castTypeName;
		}
		return this;
	}

	/// Use this SQL type name where a LOB cast target is not accepted.
	public DdlTypeBuilder narrowCastTypeName(String name) {
		this.narrowCastTypeName = requireText( name, "narrowCastTypeName" );
		this.narrowCastTypeNameSpecified = true;
		return this;
	}

	/// Resolve the cast type name from an explicit requested length.
	///
	/// The ordinary cast behavior is used when no length is available. This
	/// setting is mutually exclusive with [#castTypeNamePattern(String)].
	public DdlTypeBuilder parameterizedCastTypeName(IntFunction<String> resolver) {
		if ( castTypeNamePattern != null ) {
			throw new IllegalArgumentException(
					"parameterizedCastTypeName cannot be combined with castTypeNamePattern"
			);
		}
		this.parameterizedCastTypeName = requireNonNull( resolver, "parameterizedCastTypeName" );
		return this;
	}

	/// Add a physical type-name pattern with the given inclusive capacity.
	public DdlTypeBuilder withTypeCapacity(long capacity, String typeNamePattern) {
		if ( capacity <= 0 ) {
			throw new IllegalArgumentException( "capacity must be positive: " + capacity );
		}
		final String pattern = requireText( typeNamePattern, "typeNamePattern" );
		if ( typeCapacityEntries.putIfAbsent( capacity, pattern ) != null ) {
			throw new IllegalArgumentException( "duplicate capacity: " + capacity );
		}
		return this;
	}

	/// Build an immutable descriptor from the current configuration.
	public DdlType build() {
		if ( lobKind == LobKind.BIGGEST && typeCapacityEntries.isEmpty() ) {
			throw new IllegalArgumentException( "LobKind.BIGGEST requires at least one capacity entry" );
		}
		if ( typeCapacityEntries.isEmpty() && parameterizedCastTypeName == null ) {
			return new DdlTypeImpl(
					sqlTypeCode,
					lobKind == LobKind.ALL,
					typeNamePattern,
					castTypeNamePattern,
					castTypeName,
					narrowCastTypeName,
					dialect
			);
		}

		final var builder = CapacityDependentDdlType.builder(
				sqlTypeCode,
				toInternalLobKind( lobKind ),
				typeNamePattern,
				castTypeNamePattern,
				castTypeName,
				dialect
		).withNarrowCastTypeName( narrowCastTypeName );
		if ( parameterizedCastTypeName != null ) {
			builder.withParameterizedCastTypeName( this::resolveParameterizedCastTypeName );
		}
		for ( var entry : typeCapacityEntries.entrySet() ) {
			builder.withTypeCapacity( entry.getKey(), entry.getValue() );
		}
		return builder.build();
	}

	private String resolveParameterizedCastTypeName(int length) {
		return requireGeneratedText( parameterizedCastTypeName.apply( length ), "parameterizedCastTypeName" );
	}

	private static CapacityDependentDdlType.LobKind toInternalLobKind(LobKind lobKind) {
		return switch ( lobKind ) {
			case NONE -> CapacityDependentDdlType.LobKind.NONE;
			case BIGGEST -> CapacityDependentDdlType.LobKind.BIGGEST_LOB;
			case ALL -> CapacityDependentDdlType.LobKind.ALL_LOB;
		};
	}

	static String requireText(String value, String parameterName) {
		if ( value == null || value.isBlank() ) {
			throw new IllegalArgumentException( parameterName + " must not be null or blank" );
		}
		return value;
	}

	static String requireGeneratedText(String value, String parameterName) {
		if ( value == null || value.isBlank() ) {
			throw new IllegalStateException( parameterName + " produced a null or blank SQL type name" );
		}
		return value;
	}

	private static <T> T requireNonNull(T value, String parameterName) {
		if ( value == null ) {
			throw new IllegalArgumentException( parameterName + " must not be null" );
		}
		return value;
	}
}
