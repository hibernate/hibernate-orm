/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.SPI;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.internal.StandardDialectTestContext;
import org.hibernate.dialect.testing.spi.ContractApplicability;
import org.hibernate.dialect.testing.spi.DialectContract;
import org.hibernate.dialect.testing.spi.DialectContractProfile;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Entry point for the provider-oriented, database-free Dialect contract test
/// kit.
///
/// Add a provider-owned JUnit `@TestFactory` which returns
/// `DialectTestKit.contractTests(profile)`. The returned tests exercise
/// Hibernate's real metadata, SQM, SQL AST, and schema-generation pipelines.
/// They are compatibility smoke tests, not certification against a live
/// database.
///
/// This class is stateless and thread-safe. Each opened context is
/// thread-confined.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public final class DialectTestKit {
	private DialectTestKit() {
	}

	/// Build the complete dynamic contract suite for one provider profile.
	///
	/// @see DialectContractProfile
	@SPI(SUPPLY)
	public static DynamicContainer contractTests(DialectContractProfile profile) {
		final ProfileDescriptor descriptor = validateProfile( profile );
		final List<DynamicTest> tests = new ArrayList<>();
		for ( DialectContract contract : DialectContract.values() ) {
			final ContractApplicability applicability = Objects.requireNonNull(
					profile.applicability( contract ),
					"profile.applicability(" + contract + ")"
			);
			if ( contract.isRequired() && !applicability.isApplicable() ) {
				throw new IllegalArgumentException(
						"Required contract " + contract + " cannot be marked inapplicable"
				);
			}
			tests.add( DynamicTest.dynamicTest(
					contract.name(),
					() -> {
						assumeTrue( applicability.isApplicable(), applicability.reason() );
						runContract( contract, descriptor );
					}
			) );
		}
		return DynamicContainer.dynamicContainer( descriptor.name(), tests );
	}

	/// Open a context for provider-specific assertions outside the generic
	/// dynamic suite.
	///
	/// @see DialectContractProfile
	@SPI(SUPPLY)
	public static DialectTestContext openContext(DialectContractProfile profile) {
		validateProfile( profile );
		return new DialectTestContext( new StandardDialectTestContext( profile ) );
	}

	private static ProfileDescriptor validateProfile(DialectContractProfile profile) {
		Objects.requireNonNull( profile, "profile" );
		final String name = Objects.requireNonNull( profile.name(), "profile.name()" ).strip();
		if ( name.isEmpty() ) {
			throw new IllegalArgumentException( "Profile name must not be blank" );
		}
		final Dialect first = Objects.requireNonNull( profile.createDialect(), "profile.createDialect()" );
		final Dialect second = Objects.requireNonNull( profile.createDialect(), "profile.createDialect()" );
		if ( first == second ) {
			throw new IllegalArgumentException( "Profile must create a fresh Dialect for each invocation" );
		}
		final DatabaseVersion expectedVersion = Objects.requireNonNull(
				profile.expectedDatabaseVersion(),
				"profile.expectedDatabaseVersion()"
		);
		Map.copyOf( Objects.requireNonNull( profile.settings(), "profile.settings()" ) );
		return new ProfileDescriptor( name, profile, expectedVersion );
	}

	private static void runContract(DialectContract contract, ProfileDescriptor descriptor) {
		try ( DialectTestContext context = openContext( descriptor.profile() ) ) {
			switch ( contract ) {
				case BOOTSTRAP -> bootstrap( context, descriptor.expectedVersion() );
				case BASIC_QUERY_TRANSLATION -> basicQueryTranslation( context );
				case IDENTIFIER_AND_LITERAL_RENDERING -> identifierAndLiteralRendering( context );
				case PARAMETER_MARKER_ORDER -> parameterMarkerOrder( context );
				case TABLELESS_AND_SYNTHETIC_ROOTS -> tablelessAndSyntheticRoots( context );
				case FETCH_AND_PAGINATION -> fetchAndPagination( context );
				case LOCKING -> locking( context );
				case SCHEMA_DDL -> schemaDdl( context );
				case TEMPORARY_TABLES -> temporaryTables( context );
				case MULTI_TABLE_MUTATION -> multiTableMutation( context );
			}
		}
	}

	private static void bootstrap(DialectTestContext context, DatabaseVersion expectedVersion) {
		final DatabaseVersion actual = context.getDialect().getVersion();
		assertEquals( expectedVersion.getMajor(), actual.getMajor() );
		assertEquals( expectedVersion.getMinor(), actual.getMinor() );
		assertEquals( expectedVersion.getMicro(), actual.getMicro() );
	}

	private static void basicQueryTranslation(DialectTestContext context) {
		assertSql( context.translate( "from ContractEntity" ) );
		assertSql( context.translate( "insert into ContractEntity (id, name, quantity) select id, name, quantity from ContractEntity" ) );
		assertSql( context.translate( "update ContractEntity set quantity = 1" ) );
		assertSql( context.translate( "delete from ContractEntity" ) );
	}

	private static void identifierAndLiteralRendering(DialectTestContext context) {
		assertSql( context.translate(
				"select e.name from ContractEntity e where e.name = 'O''Reilly' and e.quantity = 7"
		) );
	}

	private static void parameterMarkerOrder(DialectTestContext context) {
		final SqlGenerationResult result = context.translate( new SqlGenerationRequest(
				"select e.id from ContractEntity e where e.id in :ids and e.name = :name",
				Long.class,
				Map.of( "ids", List.of( 1L, 2L ), "name", "test" ),
				null,
				null
		) );
		assertSql( result );
		final List<GeneratedParameter> parameters = result.statements().get( 0 ).parameters()
				.stream()
				.filter( parameter -> parameter.queryParameterName() != null )
				.toList();
		final List<Integer> idPositions = positions( parameters, "ids" );
		final int namePosition = firstPosition( parameters, "name" );
		assertEquals( List.of( 1, 2 ), idPositions );
		assertEquals( 3, namePosition );
	}

	private static void tablelessAndSyntheticRoots(DialectTestContext context) {
		assertSql( context.translate( "select 1" ) );
		assertSql( context.translate( "select e.quantity, count(e.id) from ContractEntity e group by e.quantity order by e.quantity" ) );
	}

	private static void fetchAndPagination(DialectTestContext context) {
		assertSql( context.translate( new SqlGenerationRequest(
				"from ContractEntity e order by e.id",
				null,
				Map.of(),
				new Pagination( 5, 10 ),
				null
		) ) );
	}

	private static void locking(DialectTestContext context) {
		assertSql( context.translate( new SqlGenerationRequest(
				"from ContractEntity e where e.id = 1",
				null,
				Map.of(),
				null,
				LockMode.PESSIMISTIC_WRITE
		) ) );
	}

	private static void schemaDdl(DialectTestContext context) {
		final SchemaGenerationResult result = context.generateSchema();
		assertFalse( result.createCommands().isEmpty() );
		assertFalse( result.dropCommands().isEmpty() );
	}

	private static void temporaryTables(DialectTestContext context) {
		assertNotNull( context.getDialect().getTemporaryTableExporter() );
		assertNotNull( context.getDialect().getPersistentTemporaryTableStrategy() );
	}

	private static void multiTableMutation(DialectTestContext context) {
		assertNotNull( context.getDialect().getMultiTableMutationSupport() );
		assertSql( context.translate( "update ContractEntity set name = 'updated' where quantity > 0" ) );
	}

	private static void assertSql(SqlGenerationResult result) {
		assertNotNull( result );
		assertFalse( result.statements().isEmpty() );
		for ( GeneratedStatement statement : result.statements() ) {
			assertFalse( statement.sql().isBlank() );
		}
	}

	private static int firstPosition(List<GeneratedParameter> parameters, String name) {
		return parameters.stream()
				.filter( parameter -> name.equals( parameter.queryParameterName() ) )
				.mapToInt( GeneratedParameter::jdbcPosition )
				.findFirst()
				.orElse( -1 );
	}

	private static List<Integer> positions(List<GeneratedParameter> parameters, String name) {
		return parameters.stream()
				.filter( parameter -> name.equals( parameter.queryParameterName() ) )
				.map( GeneratedParameter::jdbcPosition )
				.toList();
	}

	private record ProfileDescriptor(
			String name,
			DialectContractProfile profile,
			DatabaseVersion expectedVersion) {
	}
}
