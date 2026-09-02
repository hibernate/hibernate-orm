/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;
import org.hibernate.dialect.identity.spi.IdentityValueRetrieval;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.INSERT_RETURNING;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.INSERT_RETURNING_ROW_ID;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.UPDATE_RETURNING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the identity and general generated-values provider profiles.
///
/// @author Steve Ebersole
public class IdentityAndGeneratedValuesSupportTests {
	@Test
	void identityBaseSuppliesAllConservativeDefaults() {
		final var support = IdentityColumnSupportBase.NONE;
		assertThat( support.supportsIdentityColumns() ).isFalse();
		assertThat( support.supportsInsertSelectIdentity() ).isFalse();
		assertThat( support.hasDataTypeInIdentityColumn() ).isTrue();
		assertThat( support.appendIdentitySelectToInsert( "id", "insert into t default values" ) )
				.isEqualTo( "insert into t default values" );
		assertThat( support.getIdentityInsertString() ).isNull();
		assertThat( support.hasIdentityInsertKeyword() ).isFalse();
		assertThat( support.getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.INFERRED_GENERATED_KEYS );
		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> support.getIdentityColumnString( java.sql.Types.BIGINT ) );
		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> support.getIdentitySelectString( "t", "id", java.sql.Types.BIGINT ) );
	}

	@Test
	void identityRetrievalVocabularyAndStockExceptionsAreExact() {
		assertThat( IdentityValueRetrieval.values() ).containsExactly(
				IdentityValueRetrieval.INFERRED_GENERATED_KEYS,
				IdentityValueRetrieval.NAMED_GENERATED_KEYS,
				IdentityValueRetrieval.APPENDED_SELECT
		);
		assertThat( new H2Dialect().getIdentityColumnSupport().getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.INFERRED_GENERATED_KEYS );
		assertThat( new OracleDialect().getIdentityColumnSupport().getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.NAMED_GENERATED_KEYS );
		final var appended = new SybaseDialect().getIdentityColumnSupport();
		assertThat( appended.getIdentityValueRetrieval() ).isEqualTo( IdentityValueRetrieval.APPENDED_SELECT );
		assertThat( appended.supportsInsertSelectIdentity() ).isTrue();
		assertThat( appended.appendIdentitySelectToInsert( "id", "insert into t default values" ) )
				.isNotNull()
				.contains( "insert into t default values" );
	}

	@Test
	void identityInsertKeywordPresenceAndHsqlVersionValuesAreExact() {
		final var h2 = new H2Dialect().getIdentityColumnSupport();
		assertThat( h2.hasIdentityInsertKeyword() ).isTrue();
		assertThat( h2.getIdentityInsertString() ).isEqualTo( "default" );

		final var hsql1 = new HSQLDialect( DatabaseVersion.make( 1, 8 ) ).getIdentityColumnSupport();
		assertThat( hsql1.hasIdentityInsertKeyword() ).isTrue();
		assertThat( hsql1.getIdentityInsertString() ).isEqualTo( "null" );

		final var hsql2 = new HSQLDialect( DatabaseVersion.make( 2 ) ).getIdentityColumnSupport();
		assertThat( hsql2.hasIdentityInsertKeyword() ).isTrue();
		assertThat( hsql2.getIdentityInsertString() ).isEqualTo( "default" );
	}

	@Test
	void sybaseDriverBranchesPreserveIdentityRetrieval() {
		assertThat( sybaseDialect( "jConnect (TM) for JDBC (TM)" )
				.getIdentityColumnSupport()
				.getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.APPENDED_SELECT );
		assertThat( sybaseDialect( "jTDS Type 4 JDBC Driver for MS SQL Server and Sybase" )
				.getIdentityColumnSupport()
				.getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.INFERRED_GENERATED_KEYS );
	}

	@Test
	void generatedValuesProfileIsIndependentImmutableAndCopyable() {
		assertProfile( GeneratedValuesSupport.STANDARD );
		assertThat( GeneratedValuesSupport.STANDARD.unquoteGeneratedKeyColumnNames() ).isFalse();
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> GeneratedValuesSupport.STANDARD.getCapabilities().add( INSERT_RETURNING ) );

		final var original = GeneratedValuesSupport.builder()
				.enable( UPDATE_RETURNING, ARBITRARY_GENERATED_KEYS )
				.unquoteGeneratedKeyColumnNames( true )
				.build();
		final var copy = GeneratedValuesSupport.builder( original )
				.enable( INSERT_RETURNING )
				.disable( UPDATE_RETURNING )
				.build();

		assertProfile( original, UPDATE_RETURNING, ARBITRARY_GENERATED_KEYS );
		assertThat( original.unquoteGeneratedKeyColumnNames() ).isTrue();
		assertProfile( copy, INSERT_RETURNING, ARBITRARY_GENERATED_KEYS );
		assertThat( copy.unquoteGeneratedKeyColumnNames() ).isTrue();
	}

	@Test
	void generatedValuesProfileRejectsNullAndInconsistentInput() {
		assertThatIllegalArgumentException().isThrownBy( () -> GeneratedValuesSupport.builder( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> GeneratedValuesSupport.builder().enable( (GeneratedValuesSupport.Capability[]) null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> GeneratedValuesSupport.builder().enable( (GeneratedValuesSupport.Capability) null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> GeneratedValuesSupport.builder().disable( (GeneratedValuesSupport.Capability[]) null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> GeneratedValuesSupport.STANDARD.supports( null ) );
		assertThatIllegalArgumentException().isThrownBy(
				() -> GeneratedValuesSupport.builder().enable( INSERT_RETURNING_ROW_ID ).build()
		);
		assertThatIllegalArgumentException().isThrownBy(
				() -> GeneratedValuesSupport.builder().unquoteGeneratedKeyColumnNames( true ).build()
		);
	}

	@Test
	void maintainedDialectProfilesPreserveEveryEffectiveCombination() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getGeneratedValuesSupport() );
		assertProfile(
				new PostgreSQLDialect().getGeneratedValuesSupport(),
				INSERT_RETURNING,
				UPDATE_RETURNING,
				INSERT_RETURNING_ROW_ID
		);
		assertProfile(
				new CockroachDialect().getGeneratedValuesSupport(),
				INSERT_RETURNING,
				UPDATE_RETURNING,
				INSERT_RETURNING_ROW_ID
		);
		assertProfile( new DB2Dialect().getGeneratedValuesSupport(), INSERT_RETURNING, UPDATE_RETURNING );
		assertProfile( new DB2zDialect().getGeneratedValuesSupport(), INSERT_RETURNING, UPDATE_RETURNING );
		assertProfile( new DB2iDialect( DatabaseVersion.make( 7, 5 ) ).getGeneratedValuesSupport(), INSERT_RETURNING );
		assertProfile(
				new DB2iDialect( DatabaseVersion.make( 7, 6 ) ).getGeneratedValuesSupport(),
				INSERT_RETURNING,
				UPDATE_RETURNING
		);
		assertProfile(
				new MariaDBDialect().getGeneratedValuesSupport(),
				INSERT_RETURNING,
				INSERT_RETURNING_ROW_ID
		);
		assertProfile( new OracleDialect().getGeneratedValuesSupport(), ARBITRARY_GENERATED_KEYS );

		final var h2 = new H2Dialect().getGeneratedValuesSupport();
		assertProfile( h2, INSERT_RETURNING, UPDATE_RETURNING, ARBITRARY_GENERATED_KEYS );
		assertThat( h2.unquoteGeneratedKeyColumnNames() ).isTrue();
	}

	private static void assertProfile(
			GeneratedValuesSupport support,
			GeneratedValuesSupport.Capability... capabilities) {
		assertThat( support.getCapabilities() ).isEqualTo( Set.of( capabilities ) );
		for ( GeneratedValuesSupport.Capability capability : GeneratedValuesSupport.Capability.values() ) {
			assertThat( support.supports( capability ) )
					.as( capability.name() )
					.isEqualTo( Set.of( capabilities ).contains( capability ) );
		}
	}

	private static SybaseDialect sybaseDialect(String driverName) {
		final var info = mock( DialectResolutionInfo.class );
		when( info.getDatabaseMajorVersion() ).thenReturn( 16 );
		when( info.getDatabaseMinorVersion() ).thenReturn( 0 );
		when( info.getDriverName() ).thenReturn( driverName );
		return new SybaseDialect( info );
	}
}
