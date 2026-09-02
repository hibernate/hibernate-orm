/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;
import org.hibernate.dialect.type.spi.StandardDdlTypes;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/// @author Steve Ebersole
public class DdlTypeConstructionTests {
	private static final int CUSTOM_TYPE = 60_001;
	private static final Dialect DIALECT = new H2Dialect();
	private static final BasicTypeImpl<String> STRING_TYPE =
			new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE );

	private final DdlTypeRegistry ddlTypeRegistry = new DdlTypeRegistry( new TypeConfiguration() );

	@Test
	void simpleFactoriesAndBuilderDefaults() {
		final DdlType simple = StandardDdlTypes.simple( CUSTOM_TYPE, "fixture($l)", DIALECT );
		assertThat( simple.getSqlTypeCode() ).isEqualTo( CUSTOM_TYPE );
		assertThat( simple.getRawTypeNames() ).containsExactly( "fixture" );
		assertThat( typeName( simple, new Size( 9, 2, 17L ) ) ).isEqualTo( "fixture(17)" );
		assertThat( castName( simple, Size.nil() ) ).isEqualTo( "fixture(1048576)" );
		assertThat( simple.isLob( Size.nil() ) ).isFalse();

		final DdlType distinctCast = StandardDdlTypes.simple(
				CUSTOM_TYPE,
				"fixture($l)",
				"fixture_cast",
				DIALECT
		);
		assertThat( castName( distinctCast, Size.nil() ) ).isEqualTo( "fixture_cast" );
		assertThat( narrowCastName( distinctCast, Size.nil() ) ).isEqualTo( "fixture_cast" );

		final DdlType defaultLob = StandardDdlTypes.builder( SqlTypes.CLOB, "fixture_clob", DIALECT ).build();
		assertThat( defaultLob.isLob( Size.nil() ) ).isTrue();
	}

	@Test
	void configuresTypeAndCastPatternsIndependently() {
		final DdlType ddlType = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture($l,$p,$s)", DIALECT )
				.castTypeNamePattern( "fixture_cast($l,$p,$s)" )
				.castTypeName( "fixture_cast" )
				.narrowCastTypeName( "fixture_narrow($l)" )
				.build();

		final Size size = new Size( 9, 2, 17L );
		assertThat( typeName( ddlType, size ) ).isEqualTo( "fixture(17,9,2)" );
		assertThat( castName( ddlType, size ) ).isEqualTo( "fixture_cast(17,9,2)" );
		assertThat( castName( ddlType, Size.nil() ) ).isEqualTo( "fixture_cast" );
		assertThat( narrowCastName( ddlType, Size.length( 17 ) ) ).isEqualTo( "fixture_narrow(17)" );
	}

	@Test
	void selectsSortedInclusiveCapacitiesByLengthThenPrecision() {
		final DdlType ddlType = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture_default", DIALECT )
				.withTypeCapacity( 100, "fixture_medium($l)" )
				.withTypeCapacity( 10, "fixture_small($l)" )
				.build();

		assertThat( typeName( ddlType, Size.length( 10 ) ) ).isEqualTo( "fixture_small(10)" );
		assertThat( typeName( ddlType, Size.length( 11 ) ) ).isEqualTo( "fixture_medium(11)" );
		assertThat( typeName( ddlType, Size.length( 100 ) ) ).isEqualTo( "fixture_medium(100)" );
		assertThat( typeName( ddlType, Size.length( 101 ) ) ).isEqualTo( "fixture_default" );
		assertThat( typeName( ddlType, Size.precision( 10 ) ) ).isEqualTo( "fixture_small(-1)" );
		assertThat( typeName( ddlType, Size.precision( 11 ) ) ).isEqualTo( "fixture_medium(-1)" );
		assertThat( typeName( ddlType, Size.nil() ) ).isEqualTo( "fixture_default" );
	}

	@Test
	void appliesEveryLobPolicyAtCapacityBoundaries() {
		final DdlType none = capacityType( DdlTypeBuilder.LobKind.NONE );
		final DdlType biggest = capacityType( DdlTypeBuilder.LobKind.BIGGEST );
		final DdlType all = capacityType( DdlTypeBuilder.LobKind.ALL );

		for ( long length : new long[] { 9, 10, 11 } ) {
			assertThat( none.isLob( Size.length( length ) ) ).isFalse();
			assertThat( all.isLob( Size.length( length ) ) ).isTrue();
		}
		assertThat( biggest.isLob( Size.length( 9 ) ) ).isFalse();
		assertThat( biggest.isLob( Size.length( 10 ) ) ).isFalse();
		assertThat( biggest.isLob( Size.length( 11 ) ) ).isTrue();
	}

	@Test
	void resolvesParameterizedCastNamesAndPreservesOrdinaryFallback() {
		final AtomicInteger requestedLength = new AtomicInteger();
		final DdlType ddlType = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture($l)", DIALECT )
				.castTypeName( "fixture_cast" )
				.parameterizedCastTypeName( length -> {
					requestedLength.set( length );
					return "fixture_cast(" + length + ")";
				} )
				.withTypeCapacity( 100, "fixture($l)" )
				.build();

		assertThat( castName( ddlType, Size.length( 27 ) ) ).isEqualTo( "fixture_cast(27)" );
		assertThat( requestedLength ).hasValue( 27 );
		assertThat( castName( ddlType, Size.nil() ) ).isEqualTo( "fixture_cast" );

		final DdlType simple = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT )
				.castTypeName( "fixture_cast" )
				.parameterizedCastTypeName( length -> "fixture_cast(" + length + ")" )
				.build();
		assertThat( castName( simple, Size.length( 31 ) ) ).isEqualTo( "fixture_cast(31)" );
		assertThat( castName( simple, Size.nil() ) ).isEqualTo( "fixture_cast" );
	}

	@Test
	void buildsImmutableCapacitySnapshots() {
		final DdlTypeBuilder builder = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture_default", DIALECT )
				.withTypeCapacity( 10, "fixture_small" );
		final DdlType first = builder.build();
		builder.withTypeCapacity( 100, "fixture_medium" );
		final DdlType second = builder.build();

		assertThat( typeName( first, Size.length( 50 ) ) ).isEqualTo( "fixture_default" );
		assertThat( typeName( second, Size.length( 50 ) ) ).isEqualTo( "fixture_medium" );
	}

	@Test
	void validatesBuilderConfiguration() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, null, DIALECT )
		).withMessageContaining( "typeNamePattern" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, " ", DIALECT )
		).withMessageContaining( "typeNamePattern" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", null )
		).withMessageContaining( "dialect" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).lobKind( null )
		).withMessageContaining( "lobKind" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).castTypeName( "" )
		).withMessageContaining( "castTypeName" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).narrowCastTypeName( " " )
		).withMessageContaining( "narrowCastTypeName" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).parameterizedCastTypeName( null )
		).withMessageContaining( "parameterizedCastTypeName" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.nativeEnum( DIALECT, " ", length -> "char(" + length + ")" )
		).withMessageContaining( "castTypeName" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.nativeEnum( DIALECT, "char", null )
		).withMessageContaining( "parameterizedCastTypeName" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).withTypeCapacity( 0, "small" )
		).withMessageContaining( "positive" );
		assertThatIllegalArgumentException().isThrownBy(
				() -> StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT ).withTypeCapacity( 1, " " )
		).withMessageContaining( "typeNamePattern" );
		assertThatIllegalArgumentException().isThrownBy( () -> StandardDdlTypes.builder(
				CUSTOM_TYPE,
				"fixture",
				DIALECT
		).withTypeCapacity( 1, "small" ).withTypeCapacity( 1, "duplicate" ) ).withMessageContaining( "duplicate" );
		assertThatIllegalArgumentException().isThrownBy( () -> StandardDdlTypes.builder(
				CUSTOM_TYPE,
				"fixture",
				DIALECT
		).lobKind( DdlTypeBuilder.LobKind.BIGGEST ).build() ).withMessageContaining( "requires at least one" );
	}

	@Test
	void rejectsConflictingAndInvalidParameterizedCastResolvers() {
		assertThatIllegalArgumentException().isThrownBy( () -> StandardDdlTypes.builder(
				CUSTOM_TYPE,
				"fixture",
				DIALECT
		).castTypeNamePattern( "cast($l)" ).parameterizedCastTypeName( length -> "cast(" + length + ")" ) );
		assertThatIllegalArgumentException().isThrownBy( () -> StandardDdlTypes.builder(
				CUSTOM_TYPE,
				"fixture",
				DIALECT
		).parameterizedCastTypeName( length -> "cast(" + length + ")" ).castTypeNamePattern( "cast($l)" ) );

		final DdlType ddlType = StandardDdlTypes.builder( CUSTOM_TYPE, "fixture", DIALECT )
				.parameterizedCastTypeName( length -> " " )
				.withTypeCapacity( 10, "small" )
				.build();
		assertThatIllegalStateException().isThrownBy( () -> castName( ddlType, Size.length( 5 ) ) )
				.withMessageContaining( "parameterizedCastTypeName" );
	}

	@Test
	void createsStandardArrayWithRawOrSizedCastElements() {
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.simple( SqlTypes.VARCHAR, "varchar($l)", "varchar", DIALECT )
		);
		final BasicTypeImpl<String> elementType =
				new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE );
		final BasicArrayType<String[], String> arrayType = new BasicArrayType<>(
				elementType,
				new ArrayJdbcType( VarcharJdbcType.INSTANCE ),
				new ArrayJavaType<>( StringJavaType.INSTANCE )
		);
		final Size size = Size.length( 27 ).setArrayLength( 3 );

		assertThat( StandardDdlTypes.standardArray( DIALECT, false )
				.getCastTypeName( size, arrayType, ddlTypeRegistry ) ).isEqualTo( "varchar(27) array[3]" );
		assertThat( StandardDdlTypes.standardArray( DIALECT, true )
				.getCastTypeName( size, arrayType, ddlTypeRegistry ) ).isEqualTo( "varchar array[3]" );
	}

	@Test
	void createsAllEnumFamiliesAndCustomNativeEnumCasts() {
		final BasicTypeImpl<TestEnum> enumType =
				new BasicTypeImpl<>( new EnumJavaType<>( TestEnum.class ), EnumJdbcType.INSTANCE );

		assertThat( typeName( StandardDdlTypes.nativeEnum( DIALECT ), Size.length( 12 ) ) )
				.isEqualTo( "varchar(12)" );
		assertThat( typeName( StandardDdlTypes.nativeOrdinalEnum( DIALECT ), Size.nil() ) ).isEqualTo( "int" );
		assertThat( StandardDdlTypes.namedNativeEnum().getTypeName( Size.nil(), enumType, ddlTypeRegistry ) )
				.isEqualTo( "TestEnum" );
		assertThat( StandardDdlTypes.namedNativeOrdinalEnum().getTypeName( Size.nil(), enumType, ddlTypeRegistry ) )
				.isEqualTo( "TestEnum" );

		final DdlType customCast = StandardDdlTypes.nativeEnum(
				DIALECT,
				length -> "char(" + length + ")"
		);
		assertThat( customCast.getCastTypeName( Size.length( 12 ), enumType, ddlTypeRegistry ) )
				.isEqualTo( "char(12)" );
		assertThat( customCast.getCastTypeName( Size.nil(), enumType, ddlTypeRegistry ) ).isEqualTo( "varchar" );

		final DdlType distinctUnsizedCast = StandardDdlTypes.nativeEnum(
				DIALECT,
				"char",
				length -> "char(" + length + ")"
		);
		assertThat( distinctUnsizedCast.getCastTypeName( Size.length( 12 ), enumType, ddlTypeRegistry ) )
				.isEqualTo( "char(12)" );
		assertThat( distinctUnsizedCast.getCastTypeName( Size.nil(), enumType, ddlTypeRegistry ) ).isEqualTo( "char" );
	}

	@Test
	void preservesMySqlNativeEnumCastNames() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		new MySQLDialect().contributeTypes( () -> typeConfiguration, null );
		final DdlTypeRegistry registry = typeConfiguration.getDdlTypeRegistry();
		final DdlType ddlType = registry.getDescriptor( SqlTypes.ENUM );
		final BasicTypeImpl<TestEnum> enumType =
				new BasicTypeImpl<>( new EnumJavaType<>( TestEnum.class ), EnumJdbcType.INSTANCE );

		assertThat( ddlType.getCastTypeName( Size.nil(), enumType, registry ) ).isEqualTo( "char" );
		assertThat( ddlType.getCastTypeName( Size.length( 12 ), enumType, registry ) ).isEqualTo( "char(12)" );
		assertThat( ddlType.getCastTypeName( Size.length( 1_073_741_824L ), enumType, registry ) ).isEqualTo( "char" );
	}

	@Test
	void createsBinaryFloatAndScaleSixIntervalDescriptors() {
		assertThat( typeName( StandardDdlTypes.binaryFloat( DIALECT ), Size.precision( 10 ) ) )
				.isEqualTo( "float(3)" );
		assertThat( typeName( StandardDdlTypes.binaryFloat( "binary_float($p)", DIALECT ), Size.precision( 10 ) ) )
				.isEqualTo( "binary_float(3)" );
		assertThat( typeName( StandardDdlTypes.scale6IntervalSecond( DIALECT ), Size.precision( 2, 6 ) ) )
				.isEqualTo( "interval second(6)" );
		assertThat( typeName(
				StandardDdlTypes.scale6IntervalSecond( "duration($s)", DIALECT ),
				Size.precision( 2, 4 )
		) ).isEqualTo( "duration(4)" );
		assertThatIllegalStateException().isThrownBy(
				() -> typeName( StandardDdlTypes.scale6IntervalSecond( DIALECT ), Size.nil() )
		);
		assertThatIllegalStateException().isThrownBy(
				() -> typeName( StandardDdlTypes.scale6IntervalSecond( DIALECT ), Size.precision( 2, 7 ) )
		);
	}

	private DdlType capacityType(DdlTypeBuilder.LobKind lobKind) {
		return StandardDdlTypes.builder( CUSTOM_TYPE, "fixture_lob", DIALECT )
				.lobKind( lobKind )
				.withTypeCapacity( 10, "fixture_small" )
				.build();
	}

	private String typeName(DdlType ddlType, Size size) {
		return ddlType.getTypeName( size, null, ddlTypeRegistry );
	}

	private String castName(DdlType ddlType, Size size) {
		return ddlType.getCastTypeName( size, STRING_TYPE, ddlTypeRegistry );
	}

	private String narrowCastName(DdlType ddlType, Size size) {
		return ddlType.getNarrowCastTypeName( size, STRING_TYPE, ddlTypeRegistry );
	}

	private enum TestEnum {
		ONE,
		TWO
	}
}
