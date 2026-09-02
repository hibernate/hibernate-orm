/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import java.util.function.IntFunction;


import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.internal.ArrayDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.BinaryFloatDdlType;
import org.hibernate.type.descriptor.sql.internal.NamedNativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NamedNativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.NativeOrdinalEnumDdlTypeImpl;
import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;

import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.type.spi.DdlTypeBuilder.requireGeneratedText;
import static org.hibernate.dialect.type.spi.DdlTypeBuilder.requireText;

/// Factory for Hibernate's standard DDL type descriptor algorithms.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#registerColumnTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)].
/// Add each returned [DdlType] to the
/// [org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry] supplied through the
/// type contributions. Use [#simple(int, String, Dialect)] for a single type-
/// name pattern and [#builder(int, String, Dialect)] when cast configuration or
/// capacity-dependent selection differs.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class StandardDdlTypes {
	private StandardDdlTypes() {
	}

	/// Build a simple descriptor using one type-name pattern for declaration and
	/// casting.
	public static DdlType simple(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		return builder( sqlTypeCode, typeNamePattern, dialect ).build();
	}

	/// Build a simple descriptor with a distinct unsized cast type name.
	public static DdlType simple(
			int sqlTypeCode,
			String typeNamePattern,
			String castTypeName,
			Dialect dialect) {
		return builder( sqlTypeCode, typeNamePattern, dialect ).castTypeName( castTypeName ).build();
	}

	/// Begin configuring a simple or capacity-dependent descriptor.
	public static DdlTypeBuilder builder(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		return new DdlTypeBuilder( sqlTypeCode, typeNamePattern, dialect );
	}

	/// Build the standard SQL array descriptor.
	///
	/// @param castRawElementType whether cast targets use the raw element type
	/// name without length, precision, or scale parameters
	public static DdlType standardArray(Dialect dialect, boolean castRawElementType) {
		return new ArrayDdlTypeImpl( requireDialect( dialect ), castRawElementType );
	}

	/// Build the standard string-valued native enum descriptor.
	public static DdlType nativeEnum(Dialect dialect) {
		return new NativeEnumDdlTypeImpl( requireDialect( dialect ) );
	}

	/// Build the standard string-valued native enum descriptor with a focused
	/// length-sensitive cast type-name resolver.
	public static DdlType nativeEnum(Dialect dialect, IntFunction<String> parameterizedCastTypeName) {
		return nativeEnum( dialect, "varchar", parameterizedCastTypeName );
	}

	/// Build the standard string-valued native enum descriptor with distinct
	/// unsized and length-sensitive cast type names.
	public static DdlType nativeEnum(
			Dialect dialect,
			String castTypeName,
			IntFunction<String> parameterizedCastTypeName) {
		if ( parameterizedCastTypeName == null ) {
			throw new IllegalArgumentException( "parameterizedCastTypeName must not be null" );
		}
		return new NativeEnumDdlTypeImpl(
				requireDialect( dialect ),
				requireText( castTypeName, "castTypeName" ),
				length -> requireGeneratedText(
						parameterizedCastTypeName.apply( length ),
						"parameterizedCastTypeName"
				)
		);
	}

	/// Build the standard ordinal native enum descriptor.
	public static DdlType nativeOrdinalEnum(Dialect dialect) {
		return new NativeOrdinalEnumDdlTypeImpl( requireDialect( dialect ) );
	}

	/// Build the standard named string-valued native enum descriptor.
	public static DdlType namedNativeEnum() {
		return new NamedNativeEnumDdlTypeImpl();
	}

	/// Build the standard named ordinal native enum descriptor.
	public static DdlType namedNativeOrdinalEnum() {
		return new NamedNativeOrdinalEnumDdlTypeImpl();
	}

	/// Build the standard binary-precision floating-point descriptor.
	public static DdlType binaryFloat(Dialect dialect) {
		return new BinaryFloatDdlType( requireDialect( dialect ) );
	}

	/// Build a binary-precision floating-point descriptor with a custom type-
	/// name pattern.
	public static DdlType binaryFloat(String typeNamePattern, Dialect dialect) {
		return new BinaryFloatDdlType( requireText( typeNamePattern, "typeNamePattern" ), requireDialect( dialect ) );
	}

	/// Build the standard interval-second descriptor limited to scale six.
	public static DdlType scale6IntervalSecond(Dialect dialect) {
		return new Scale6IntervalSecondDdlType( requireDialect( dialect ) );
	}

	/// Build a scale-six interval-second descriptor with a custom type-name
	/// pattern.
	public static DdlType scale6IntervalSecond(String typeNamePattern, Dialect dialect) {
		return new Scale6IntervalSecondDdlType(
				requireText( typeNamePattern, "typeNamePattern" ),
				requireDialect( dialect )
		);
	}

	private static Dialect requireDialect(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
		return dialect;
	}
}
