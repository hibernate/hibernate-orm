/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.Length;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BlobJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.DurationJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the immutable type-sizing profile and maintained Dialect behavior.
///
/// @author Steve Ebersole
public class TypeSizingProfileTests {
	private static final TypeSizingProfile CONSUMER_PROFILE = TypeSizingProfile.builder()
			.defaultDecimalPrecision( 41 )
			.defaultTimestampPrecision( 7 )
			.defaultIntervalSecondScale( 5 )
			.defaultLobLength( 111 )
			.floatPrecision( 23 )
			.doublePrecision( 52 )
			.maxVarcharLength( 10 ).maxVarcharCapacity( 20 )
			.maxNVarcharLength( 11 ).maxNVarcharCapacity( 21 )
			.maxVarbinaryLength( 12 ).maxVarbinaryCapacity( 22 )
			.build();

	private static final Dialect CONSUMER_DIALECT = new Dialect( DatabaseVersion.make( 1 ) ) {
		@Override
		public TypeSizingProfile getTypeSizingProfile() {
			return CONSUMER_PROFILE;
		}
	};

	@Test
	void standardValuesAndNoChangeBuilder() {
		assertProfile(
				TypeSizingProfile.STANDARD,
				38, 6, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				Length.LONG32, Length.LONG32, Length.LONG32,
				Length.LONG32, Length.LONG32, Length.LONG32
		);
		assertSameValues( TypeSizingProfile.STANDARD, TypeSizingProfile.builder().build() );
	}

	@Test
	void settersAreIndependentAndCopyEveryResolvedValue() {
		final TypeSizingProfile.Builder builder = TypeSizingProfile.builder()
				.defaultDecimalPrecision( 41 )
				.defaultTimestampPrecision( 0 )
				.defaultIntervalSecondScale( 0 )
				.defaultLobLength( 101 )
				.floatPrecision( 70 )
				.doublePrecision( 3 )
				.maxVarcharLength( 11 ).maxVarcharCapacity( 21 )
				.maxNVarcharLength( 12 ).maxNVarcharCapacity( 22 )
				.maxVarbinaryLength( 13 ).maxVarbinaryCapacity( 23 );
		final TypeSizingProfile first = builder.build();

		assertProfile( first, 41, 0, 0, 101, 70, 3, 11, 12, 13, 21, 22, 23 );
		assertSameValues( first, TypeSizingProfile.builder( first ).build() );

		builder.defaultDecimalPrecision( 42 ).maxVarcharLength( 10 );
		assertProfile( first, 41, 0, 0, 101, 70, 3, 11, 12, 13, 21, 22, 23 );
		assertProfile( builder.build(), 42, 0, 0, 101, 70, 3, 10, 12, 13, 21, 22, 23 );
	}

	@Test
	void unsupportedIsUniformAcrossLengthAndCapacityFamilies() {
		final TypeSizingProfile unsupported = TypeSizingProfile.builder()
				.maxVarcharLength( TypeSizingProfile.UNSUPPORTED )
				.maxVarcharCapacity( TypeSizingProfile.UNSUPPORTED )
				.maxNVarcharLength( TypeSizingProfile.UNSUPPORTED )
				.maxNVarcharCapacity( 100 )
				.maxVarbinaryLength( TypeSizingProfile.UNSUPPORTED )
				.maxVarbinaryCapacity( TypeSizingProfile.UNSUPPORTED )
				.build();

		assertThat( unsupported.maxVarcharLength() ).isEqualTo( TypeSizingProfile.UNSUPPORTED );
		assertThat( unsupported.maxVarcharCapacity() ).isEqualTo( TypeSizingProfile.UNSUPPORTED );
		assertThat( unsupported.maxNVarcharLength() ).isEqualTo( TypeSizingProfile.UNSUPPORTED );
		assertThat( unsupported.maxNVarcharCapacity() ).isEqualTo( 100 );
		assertThat( unsupported.maxVarbinaryLength() ).isEqualTo( TypeSizingProfile.UNSUPPORTED );
		assertThat( unsupported.maxVarbinaryCapacity() ).isEqualTo( TypeSizingProfile.UNSUPPORTED );
	}

	@Test
	void invalidValuesAndRelationshipsAreRejected() {
		assertInvalid( TypeSizingProfile.builder().defaultDecimalPrecision( 0 ), "defaultDecimalPrecision" );
		assertInvalid( TypeSizingProfile.builder().defaultTimestampPrecision( -1 ), "defaultTimestampPrecision" );
		assertInvalid( TypeSizingProfile.builder().defaultIntervalSecondScale( -1 ), "defaultIntervalSecondScale" );
		assertInvalid( TypeSizingProfile.builder().defaultLobLength( 0 ), "defaultLobLength" );
		assertInvalid( TypeSizingProfile.builder().floatPrecision( 0 ), "floatPrecision" );
		assertInvalid( TypeSizingProfile.builder().doublePrecision( 0 ), "doublePrecision" );
		assertInvalid( TypeSizingProfile.builder().maxVarcharLength( 0 ), "maxVarcharLength" );
		assertInvalid( TypeSizingProfile.builder().maxNVarcharLength( -2 ), "maxNVarcharLength" );
		assertInvalid( TypeSizingProfile.builder().maxVarbinaryCapacity( 0 ), "maxVarbinaryCapacity" );
		assertInvalid(
				TypeSizingProfile.builder().maxVarcharLength( 20 ).maxVarcharCapacity( 19 ),
				"maxVarcharCapacity"
		);
		assertInvalid(
				TypeSizingProfile.builder().maxNVarcharLength( 20 ).maxNVarcharCapacity( TypeSizingProfile.UNSUPPORTED ),
				"maxNVarcharCapacity"
		);
		assertInvalid(
				TypeSizingProfile.builder().maxVarbinaryLength( 20 ).maxVarbinaryCapacity( 19 ),
				"maxVarbinaryCapacity"
		);
		assertThatIllegalArgumentException()
				.isThrownBy( () -> TypeSizingProfile.builder( null ) )
				.withMessageContaining( "base" );
	}

	@Test
	void javaAndJdbcTypeConsumersReadTheSuppliedProfile() {
		final JdbcType decimal = jdbcType( SqlTypes.DECIMAL, false );
		final JdbcType floating = jdbcType( SqlTypes.FLOAT, true );
		final JdbcType timestamp = jdbcType( SqlTypes.TIMESTAMP, false );
		final JdbcType interval = jdbcType( SqlTypes.INTERVAL_SECOND, false );

		assertThat( BigDecimalJavaType.INSTANCE.getDefaultSqlPrecision( CONSUMER_DIALECT, decimal ) ).isEqualTo( 41 );
		assertThat( FloatJavaType.INSTANCE.getDefaultSqlPrecision( CONSUMER_DIALECT, floating ) ).isEqualTo( 23 );
		assertThat( DoubleJavaType.INSTANCE.getDefaultSqlPrecision( CONSUMER_DIALECT, floating ) ).isEqualTo( 52 );
		assertThat( LocalDateTimeJavaType.INSTANCE.getDefaultSqlPrecision( CONSUMER_DIALECT, timestamp ) ).isEqualTo( 7 );
		assertThat( DurationJavaType.INSTANCE.getDefaultSqlScale( CONSUMER_DIALECT, interval ) ).isEqualTo( 5 );
		assertThat( BlobJavaType.INSTANCE.getDefaultSqlLength( CONSUMER_DIALECT, decimal ) ).isEqualTo( 111 );
		assertThat( ClobJavaType.INSTANCE.getDefaultSqlLength( CONSUMER_DIALECT, decimal ) ).isEqualTo( 111 );

		final JdbcTypeIndicators varchar = indicators( false, 21 );
		final JdbcTypeIndicators nvarchar = indicators( true, 22 );
		final JdbcTypeIndicators varbinary = indicators( false, 23 );
		assertThat( new VarcharCapacityProbe().exceedsCapacity( varchar ) ).isTrue();
		assertThat( new VarcharCapacityProbe().exceedsCapacity( nvarchar ) ).isTrue();
		assertThat( new VarbinaryCapacityProbe().exceedsCapacity( varbinary ) ).isTrue();
	}

	@Test
	void maintainedProfilesPreserveEffectiveSizingTuples() {
		assertDialectProfile( new Dialect( DatabaseVersion.make( 1 ) ) {}, 38, 6, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32 );
		assertDialectProfile( new DB2Dialect(), 31, 6, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				32_672, 32_672, 32_672, 32_672, 32_672, 32_672 );
		assertDialectProfile( new HANADialect(), 34, 7, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				5000, 5000, 5000, 5000, 5000, 5000 );
		assertDialectProfile( new H2Dialect(), 38, 6, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				1_048_576, 1_048_576, 1_048_576, 1_048_576, 1_048_576, 1_048_576 );
		assertDialectProfile( new SQLServerDialect(), 38, 7, 9, Length.LONG32, 24, 53,
				8000, 4000, 8000, 8000, 4000, 8000 );
		assertDialectProfile( new PostgreSQLDialect(), 38, 6, 6, Size.DEFAULT_LOB_LENGTH, 24, 53,
				10_485_760, 10_485_760, Length.LONG32, 1_073_741_824, 10_485_760, Length.LONG32 );
		assertDialectProfile( new CockroachDialect(), 38, 6, 6, Size.DEFAULT_LOB_LENGTH, 24, 53,
				Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32, Length.LONG32 );
		assertDialectProfile( new SpannerDialect(), 38, 6, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				2_621_440, 2_621_440, 10_485_760, 2_621_440, 2_621_440, 10_485_760 );
		assertDialectProfile( new SpannerPostgreSQLDialect(), 38, 6, 6, Size.DEFAULT_LOB_LENGTH, 24, 53,
				2_621_440, 2_621_440, 10_485_760, 1_073_741_824, 2_621_440, 10_485_760 );
		assertDialectProfile( new OracleDialect(), 38, 9, 9, Size.DEFAULT_LOB_LENGTH, 24, 53,
				4000, 4000, 2000, 4000, 4000, 2000 );
		assertDialectProfile( new MySQLDialect(), 38, 6, 9, Length.LONG32, 23, 53,
				16_383, 16_383, 65_535, 16_383, 16_383, 65_535 );
		assertDialectProfile( new SybaseASEDialect(), 38, 6, 9, Length.LONG32, 15, 48,
				16_384, 16_384, 16_384, 16_384, 16_384, 16_384 );
	}

	private static void assertInvalid(TypeSizingProfile.Builder builder, String member) {
		assertThatIllegalArgumentException()
				.isThrownBy( builder::build )
				.withMessageContaining( member );
	}

	private static JdbcType jdbcType(int ddlTypeCode, boolean floating) {
		final JdbcType jdbcType = mock( JdbcType.class );
		when( jdbcType.getDdlTypeCode() ).thenReturn( ddlTypeCode );
		when( jdbcType.isFloat() ).thenReturn( floating );
		return jdbcType;
	}

	private static JdbcTypeIndicators indicators(boolean nationalized, long length) {
		final JdbcTypeIndicators indicators = mock( JdbcTypeIndicators.class );
		when( indicators.getDialect() ).thenReturn( CONSUMER_DIALECT );
		when( indicators.isNationalized() ).thenReturn( nationalized );
		when( indicators.getColumnLength() ).thenReturn( length );
		return indicators;
	}

	private static class VarcharCapacityProbe extends VarcharJdbcType {
		private boolean exceedsCapacity(JdbcTypeIndicators indicators) {
			return shouldUseMaterializedLob( indicators );
		}
	}

	private static class VarbinaryCapacityProbe extends VarbinaryJdbcType {
		private boolean exceedsCapacity(JdbcTypeIndicators indicators) {
			return shouldUseMaterializedLob( indicators );
		}
	}

	private static void assertDialectProfile(
			Dialect dialect,
			int decimal,
			int timestamp,
			int interval,
			long lob,
			int floatPrecision,
			int doublePrecision,
			int varcharLength,
			int nvarcharLength,
			int varbinaryLength,
			int varcharCapacity,
			int nvarcharCapacity,
			int varbinaryCapacity) {
		assertThat( dialect.getTypeSizingProfile() ).isSameAs( dialect.getTypeSizingProfile() );
		assertProfile( dialect.getTypeSizingProfile(), decimal, timestamp, interval, lob,
				floatPrecision, doublePrecision, varcharLength, nvarcharLength, varbinaryLength,
				varcharCapacity, nvarcharCapacity, varbinaryCapacity );
	}

	private static void assertSameValues(TypeSizingProfile expected, TypeSizingProfile actual) {
		assertProfile( actual, expected.defaultDecimalPrecision(), expected.defaultTimestampPrecision(),
				expected.defaultIntervalSecondScale(), expected.defaultLobLength(), expected.floatPrecision(),
				expected.doublePrecision(), expected.maxVarcharLength(), expected.maxNVarcharLength(),
				expected.maxVarbinaryLength(), expected.maxVarcharCapacity(), expected.maxNVarcharCapacity(),
				expected.maxVarbinaryCapacity() );
	}

	private static void assertProfile(
			TypeSizingProfile profile,
			int decimal,
			int timestamp,
			int interval,
			long lob,
			int floatPrecision,
			int doublePrecision,
			int varcharLength,
			int nvarcharLength,
			int varbinaryLength,
			int varcharCapacity,
			int nvarcharCapacity,
			int varbinaryCapacity) {
		assertThat( profile.defaultDecimalPrecision() ).isEqualTo( decimal );
		assertThat( profile.defaultTimestampPrecision() ).isEqualTo( timestamp );
		assertThat( profile.defaultIntervalSecondScale() ).isEqualTo( interval );
		assertThat( profile.defaultLobLength() ).isEqualTo( lob );
		assertThat( profile.floatPrecision() ).isEqualTo( floatPrecision );
		assertThat( profile.doublePrecision() ).isEqualTo( doublePrecision );
		assertThat( profile.maxVarcharLength() ).isEqualTo( varcharLength );
		assertThat( profile.maxNVarcharLength() ).isEqualTo( nvarcharLength );
		assertThat( profile.maxVarbinaryLength() ).isEqualTo( varbinaryLength );
		assertThat( profile.maxVarcharCapacity() ).isEqualTo( varcharCapacity );
		assertThat( profile.maxNVarcharCapacity() ).isEqualTo( nvarcharCapacity );
		assertThat( profile.maxVarbinaryCapacity() ).isEqualTo( varbinaryCapacity );
	}
}
