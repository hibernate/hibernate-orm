/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Method;
import java.sql.Types;
import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable array-support provider contract, maintained Dialect
/// profiles, and each independently selected consumer path.
///
/// @author Steve Ebersole
public class ArraySupportTests {
	private static final ArraySupport MIXED = ArraySupport.builder()
			.capabilities( ArraySupport.Capability.ARRAY_CONSTRUCTOR )
			.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.ARRAY )
			.build();

	@Test
	void constantsAndBuildersExposeImmutableIndependentDimensions() {
		assertProfile(
				ArraySupport.NONE,
				ArraySupport.MultiValuedParameterStrategy.EXPANDED
		);
		assertProfile(
				ArraySupport.STANDARD,
				ArraySupport.MultiValuedParameterStrategy.ARRAY,
				ArraySupport.Capability.STANDARD_ARRAY,
				ArraySupport.Capability.ARRAY_CONSTRUCTOR
		);

		final ArraySupport copied = ArraySupport.builder( ArraySupport.STANDARD )
				.capability( ArraySupport.Capability.ARRAY_CONSTRUCTOR, false )
				.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.EXPANDED )
				.build();
		assertProfile(
				copied,
				ArraySupport.MultiValuedParameterStrategy.EXPANDED,
				ArraySupport.Capability.STANDARD_ARRAY
		);
		assertProfile(
				ArraySupport.STANDARD,
				ArraySupport.MultiValuedParameterStrategy.ARRAY,
				ArraySupport.Capability.STANDARD_ARRAY,
				ArraySupport.Capability.ARRAY_CONSTRUCTOR
		);
		assertProfile(
				MIXED,
				ArraySupport.MultiValuedParameterStrategy.ARRAY,
				ArraySupport.Capability.ARRAY_CONSTRUCTOR
		);
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> copied.getCapabilities().add( ArraySupport.Capability.ARRAY_CONSTRUCTOR ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> ArraySupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ArraySupport.builder().capabilities( (ArraySupport.Capability[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ArraySupport.builder().capabilities( ArraySupport.Capability.STANDARD_ARRAY, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ArraySupport.builder().capability( null, true ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ArraySupport.builder().multiValuedParameterStrategy( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> ArraySupport.STANDARD.supports( null ) );
	}

	@Test
	void maintainedDialectsPreserveEveryArrayDimension() {
		assertProfile(
				new Dialect( DatabaseVersion.make( 1 ) ) {
				}.getArraySupport(),
				ArraySupport.MultiValuedParameterStrategy.EXPANDED
		);
		assertProfile(
				new H2Dialect().getArraySupport(),
				ArraySupport.MultiValuedParameterStrategy.EXPANDED,
				ArraySupport.Capability.STANDARD_ARRAY,
				ArraySupport.Capability.ARRAY_CONSTRUCTOR
		);
		for ( Dialect dialect : List.of(
				new HSQLDialect(),
				new PostgreSQLDialect(),
				new CockroachDialect() ) ) {
			assertProfile(
					dialect.getArraySupport(),
					ArraySupport.MultiValuedParameterStrategy.ARRAY,
					ArraySupport.Capability.STANDARD_ARRAY,
					ArraySupport.Capability.ARRAY_CONSTRUCTOR
			);
		}
		assertProfile(
				new SpannerDialect().getArraySupport(),
				ArraySupport.MultiValuedParameterStrategy.ARRAY,
				ArraySupport.Capability.STANDARD_ARRAY
		);
	}

	@Test
	@SuppressWarnings("NullAway")
	void standardArrayCapabilitySelectsTypeDefaultsAndLiteralRendering() {
		final Dialect standard = arrayDialect( ArraySupport.STANDARD );
		assertThat( standard.getArrayTypeName( "Integer", "integer", null ) ).isEqualTo( "integer array" );
		assertThat( standard.getArrayTypeName( "Integer", "integer", 4 ) ).isEqualTo( "integer array[4]" );
		assertThat( standard.getPreferredSqlTypeCodeForArray() ).isEqualTo( Types.ARRAY );

		final StringBuilder sql = new StringBuilder();
		standard.appendArrayLiteral(
				new StringBuilderSqlAppender( sql ),
				new Object[] { 1, null, 2 },
				(appender, value, dialect, options) -> appender.appendSql( value.toString() ),
				null
		);
		assertThat( sql ).hasToString( "ARRAY[1,null,2]" );
	}

	@Test
	@SuppressWarnings("NullAway")
	void absentStandardArrayCapabilitySelectsNonstandardDefaultsAndLiteralRejection() {
		final Dialect nonstandard = arrayDialect( MIXED );
		assertThat( nonstandard.getArrayTypeName( "Integer", "integer", null ) ).isNull();
		assertThat( nonstandard.getPreferredSqlTypeCodeForArray() ).isEqualTo( Types.VARBINARY );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> nonstandard.appendArrayLiteral(
						new StringBuilderSqlAppender( new StringBuilder() ),
						new Object[] { 1 },
						(appender, value, dialect, options) -> appender.appendSql( value.toString() ),
						null
				) )
				.withMessageContaining( "does not support array literals" );
	}

	@Test
	void mixedProfileSelectsConstructorEmulationAndArrayBindingIndependently() throws Exception {
		final Dialect mixed = arrayDialect( MIXED );
		assertThat( usesRecursiveArrayAndRowEmulation( mixed ) ).isTrue();
		assertThat( MultiKeyLoadHelper.supportsSqlArrayType( mixed ) ).isTrue();

		final ArraySupport standardSyntaxExpandedParameters = ArraySupport.builder( ArraySupport.STANDARD )
				.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.EXPANDED )
				.build();
		final Dialect expanded = arrayDialect( standardSyntaxExpandedParameters );
		assertThat( usesRecursiveArrayAndRowEmulation( expanded ) ).isTrue();
		assertThat( MultiKeyLoadHelper.supportsSqlArrayType( expanded ) ).isFalse();

		final ArraySupport arrayBindingWithoutSyntax = ArraySupport.builder()
				.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.ARRAY )
				.build();
		final Dialect noSyntax = arrayDialect( arrayBindingWithoutSyntax );
		assertThat( usesRecursiveArrayAndRowEmulation( noSyntax ) ).isFalse();
		assertThat( MultiKeyLoadHelper.supportsSqlArrayType( noSyntax ) ).isTrue();
	}

	private static boolean usesRecursiveArrayAndRowEmulation(Dialect dialect) throws Exception {
		final Method method = AbstractSqlAstTranslator.class
				.getDeclaredMethod( "supportsRecursiveClauseArrayAndRowEmulation" );
		method.setAccessible( true );
		return (boolean) method.invoke( translator( dialect ) );
	}

	private static TestingTranslator translator(Dialect dialect) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final SessionFactoryOptions sessionFactoryOptions = mock( SessionFactoryOptions.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getSessionFactoryOptions() ).thenReturn( sessionFactoryOptions );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static Dialect arrayDialect(ArraySupport support) {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public ArraySupport getArraySupport() {
				return support;
			}

			@Override
			public CteSupport getCteSupport() {
				return CteSupport.builder()
						.placement( CteSupport.Placement.TOP_LEVEL )
						.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
						.supportsRecursiveClauseArrayAndRowEmulation( true )
						.build();
			}

			@Override
			public RowValueSupport getRowValueSupport() {
				return RowValueSupport.builder( super.getRowValueSupport() )
						.feature( RowValueSupport.Feature.ROW_CONSTRUCTOR, true )
						.build();
			}
		};
	}

	private static void assertProfile(
			ArraySupport profile,
			ArraySupport.MultiValuedParameterStrategy strategy,
			ArraySupport.Capability... capabilities) {
		assertThat( profile.getCapabilities() ).containsExactlyInAnyOrder( capabilities );
		assertThat( profile.getMultiValuedParameterStrategy() ).isEqualTo( strategy );
	}

	private static class TestingTranslator extends AbstractSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}
	}
}
