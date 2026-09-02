/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.AzureSQLServerDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the top-level column-size strategy and maintained provider behavior.
///
/// @author Steve Ebersole
public class SizeStrategyTests {
	private final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
	private final StandardSizeStrategy strategy = new StandardSizeStrategy( dialect );

	@Test
	void sizeOverloadDelegatesWithoutMutatingItsInput() {
		final Size input = new Size( 12, 3, 45L );
		final SizeStrategy capturing = (jdbcType, javaType, precision, scale, length) ->
				new Size( precision + 1, scale + 1, length + 1 );

		final Size result = capturing.resolveSize( mock( JdbcType.class ), mock( JavaType.class ), input );

		assertThat( result ).isNotSameAs( input );
		assertThat( result.getPrecision() ).isEqualTo( 13 );
		assertThat( result.getScale() ).isEqualTo( 4 );
		assertThat( result.getLength() ).isEqualTo( 46 );
		assertThat( input.getPrecision() ).isEqualTo( 12 );
		assertThat( input.getScale() ).isEqualTo( 3 );
		assertThat( input.getLength() ).isEqualTo( 45 );
	}

	@Test
	void characterDefaultsAndExplicitLengthsArePreserved() {
		final JdbcType jdbcType = jdbcType( SqlTypes.VARCHAR );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getDefaultSqlLength( dialect, jdbcType ) ).thenReturn( 42L );

		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, null ).getLength() )
				.isEqualTo( 42 );
		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, Size.DEFAULT_LENGTH ).getLength() )
				.isEqualTo( 42 );
		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, 17L ).getLength() )
				.isEqualTo( 17 );
	}

	@Test
	void longTypeUsesTheJavaTypeLongLength() {
		final JdbcType jdbcType = jdbcType( SqlTypes.LONGVARBINARY );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getLongSqlLength() ).thenReturn( 12_345L );

		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, null ).getLength() )
				.isEqualTo( 12_345 );
	}

	@Test
	void numericDefaultsAndExplicitValuesArePreserved() {
		final JdbcType jdbcType = jdbcType( SqlTypes.NUMERIC );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getDefaultSqlPrecision( dialect, jdbcType ) ).thenReturn( 19 );
		when( javaType.getDefaultSqlScale( dialect, jdbcType ) ).thenReturn( 2 );

		final Size defaults = strategy.resolveSize( jdbcType, javaType, null, null, null );
		assertThat( defaults.getPrecision() ).isEqualTo( 19 );
		assertThat( defaults.getScale() ).isEqualTo( 2 );

		final Size explicit = strategy.resolveSize( jdbcType, javaType, 31, 7, null );
		assertThat( explicit.getPrecision() ).isEqualTo( 31 );
		assertThat( explicit.getScale() ).isEqualTo( 7 );
	}

	@Test
	void floatingPointPrecisionIsConvertedAndNonzeroScaleIsRejected() {
		final JdbcType jdbcType = jdbcType( SqlTypes.FLOAT );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getDefaultSqlPrecision( dialect, jdbcType ) ).thenReturn( 24 );

		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, null ).getPrecision() )
				.isEqualTo( 24 );
		assertThat( strategy.resolveSize( jdbcType, javaType, 10, 0, null ).getPrecision() )
				.isEqualTo( 34 );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> strategy.resolveSize( jdbcType, javaType, null, 1, null ) )
				.withMessageContaining( "floating point" );
	}

	@Test
	void temporalPrecisionIsPreservedAndNonzeroScaleIsRejected() {
		final JdbcType jdbcType = jdbcType( SqlTypes.TIMESTAMP );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getDefaultSqlPrecision( dialect, jdbcType ) ).thenReturn( 6 );

		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, 99L ).getPrecision() )
				.isEqualTo( 6 );
		assertThat( strategy.resolveSize( jdbcType, javaType, null, null, 99L ).getLength() )
				.isNull();
		assertThatIllegalArgumentException()
				.isThrownBy( () -> strategy.resolveSize( jdbcType, javaType, null, 1, null ) )
				.withMessageContaining( "time or timestamp" );
	}

	@Test
	void providerSubclassMaySpecializeOneCaseAndDelegateTheRest() {
		final JdbcType bitType = jdbcType( SqlTypes.BIT );
		final JdbcType varcharType = jdbcType( SqlTypes.VARCHAR );
		final JavaType<?> javaType = mock( JavaType.class );
		when( javaType.getDefaultSqlLength( dialect, varcharType ) ).thenReturn( 73L );

		final SizeStrategy custom = new StandardSizeStrategy( dialect ) {
			@Override
			public Size resolveSize(
					JdbcType jdbcType,
					JavaType<?> javaType,
					Integer precision,
					Integer scale,
					Long length) {
				if ( jdbcType.getDdlTypeCode() == SqlTypes.BIT && length != null ) {
					return Size.length( Math.min( length, 64 ) );
				}
				return super.resolveSize( jdbcType, javaType, precision, scale, length );
			}
		};

		assertThat( custom.resolveSize( bitType, javaType, null, null, 100L ).getLength() )
				.isEqualTo( 64 );
		assertThat( custom.resolveSize( varcharType, javaType, null, null, null ).getLength() )
				.isEqualTo( 73 );
	}

	@Test
	void nullDialectIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new StandardSizeStrategy( null ) )
				.withMessageContaining( "dialect" );
	}

	@Test
	void maintainedOverridesAndInheritedStrategiesAreStableStandardStrategies() {
		assertStableStandardStrategy( new MySQLDialect() );
		assertStableStandardStrategy( new SQLServerDialect() );
		assertStableStandardStrategy( new SybaseASEDialect() );
		assertStableStandardStrategy( new MariaDBDialect() );
		assertStableStandardStrategy( new AzureSQLServerDialect() );
	}

	private static JdbcType jdbcType(int ddlTypeCode) {
		final JdbcType jdbcType = mock( JdbcType.class );
		when( jdbcType.getDdlTypeCode() ).thenReturn( ddlTypeCode );
		return jdbcType;
	}

	private static void assertStableStandardStrategy(Dialect dialect) {
		assertThat( dialect.getSizeStrategy() ).isInstanceOf( StandardSizeStrategy.class );
		assertThat( dialect.getSizeStrategy() ).isSameAs( dialect.getSizeStrategy() );
	}
}
