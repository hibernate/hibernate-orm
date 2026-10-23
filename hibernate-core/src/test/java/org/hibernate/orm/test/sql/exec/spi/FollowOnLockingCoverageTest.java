package org.hibernate.orm.test.sql.exec.spi;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Identifier coverage and ordered diagnostics independent of JDBC materialization.
///
/// @author Steve Ebersole
@Jira("https://hibernate.atlassian.net/browse/HHH-21027")
class FollowOnLockingCoverageTest {
	@Test
	void reportsAllMissingIdentifiersInInputOrder() {
		final var fixture = new Fixture();
		final var expected = fixture.keys( 30, 10, 20, 40, 30 );
		final var rows = rows( 40, 10, 10 );
		final var failure = catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( expected, rows ) );
		assertThat( failure.getEntityName() ).isEqualTo( "CoverageItem" );
		assertThat( failure.getIdentifier() ).isEqualTo( 30 );
		assertThat( failure ).hasMessageStartingWith(
				"Follow-on locking failed for entity 'CoverageItem' on table 'coverage_item': missing identifiers [30, 20]" );
		assertThat( failure.getSuppressed() ).isEmpty();
		assertThat( expected ).extracting( EntityKey::getIdentifier ).containsExactly( 30, 10, 20, 40, 30 );
		assertThat( rows ).hasSize( 3 );
		verify( fixture.statistics ).optimisticFailure( "CoverageItem" );
	}

	@Test
	void completeCoverageIgnoresOrderAndDuplicateRows() {
		final var fixture = new Fixture();
		assertThatCode( () -> fixture.validate( fixture.keys( 30, 10, 20, 30 ), rows( 20, 30, 10, 10 ) ) )
				.doesNotThrowAnyException();
		verifyNoInteractions( fixture.statistics );
	}

	@Test
	void duplicateRowsCannotConcealMissingIdentifier() {
		final var fixture = new Fixture();
		final var failure = catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( fixture.keys( 1, 2 ), rows( 1, 1 ) ) );
		assertThat( failure.getIdentifier() ).isEqualTo( 2 );
		assertThat( failure ).hasMessageContaining( "missing identifiers [2]" );
	}

	@Test
	void allMissingKeysUseTheSameStaleFailure() {
		final var fixture = new Fixture();
		final var failure = catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( fixture.keys( 30, 10, 20 ), List.of() ) );
		assertThat( failure.getIdentifier() ).isEqualTo( 30 );
		assertThat( failure ).hasMessageContaining( "missing identifiers [30, 10, 20]" );
		verify( fixture.statistics ).optimisticFailure( "CoverageItem" );
	}

	@Test
	void optionalTablesAllowMissingAndPartialResults() {
		final var fixture = new Fixture();
		when( fixture.table.isOptional() ).thenReturn( true );
		when( fixture.table.isIdentifierTable() ).thenReturn( false );
		assertThatCode( () -> fixture.validate( fixture.keys( 1, 2 ), List.of() ) ).doesNotThrowAnyException();
		assertThatCode( () -> fixture.validate( fixture.keys( 1, 2 ), rows( 2 ) ) ).doesNotThrowAnyException();
		verifyNoInteractions( fixture.session, fixture.statistics );
	}

	@Test
	void identifierTableIsAlwaysRequired() {
		final var fixture = new Fixture();
		when( fixture.table.isOptional() ).thenReturn( true );
		final var failure = catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( fixture.keys( 1 ), List.of() ) );
		assertThat( failure.getIdentifier() ).isEqualTo( 1 );
	}

	@Test
	void skipLockedIsExcludedFromStaleClassification() {
		final var fixture = new Fixture();
		assertThatCode( () -> LockingHelper.validateAcquiredRows(
				fixture.table, fixture.persister, fixture.keys( 1, 2 ), List.of(), SKIP_LOCKED, fixture.session ) )
				.doesNotThrowAnyException();
		assertThatCode( () -> LockingHelper.validateAcquiredRows(
				fixture.table, fixture.persister, fixture.keys( 1, 2 ), rows( 1 ), SKIP_LOCKED, fixture.session ) )
				.doesNotThrowAnyException();
		verifyNoInteractions( fixture.session, fixture.statistics );
	}

	@Test
	void usesMappedIdentifierEqualityForMatchingAndDeduplication() {
		final var fixture = new Fixture();
		final var expected = fixture.keys( "Alpha", "ALPHA", "Beta" );
		assertThatCode( () -> fixture.validate( expected, rows( "BETA", "alpha" ) ) ).doesNotThrowAnyException();
		final var failure = catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( expected, rows( "BETA" ) ) );
		assertThat( failure.getIdentifier() ).isEqualTo( "Alpha" );
		assertThat( failure ).hasMessageContaining( "missing identifiers [Alpha]" );
	}

	@Test
	void statisticsCanBeDisabled() {
		final var fixture = new Fixture();
		when( fixture.statistics.isStatisticsEnabled() ).thenReturn( false );
		assertThat( catchThrowableOfType( StaleObjectStateException.class,
				() -> fixture.validate( fixture.keys( 1 ), List.of() ) ) ).isNotNull();
		verify( fixture.statistics, never() ).optimisticFailure( any() );
	}

	@Test
	void noExpectedEntitiesIsSuccessful() {
		final var fixture = new Fixture();
		assertThatCode( () -> fixture.validate( List.of(), List.of() ) ).doesNotThrowAnyException();
		verifyNoInteractions( fixture.statistics );
	}

	private static List<Object[]> rows(Object... identifiers) {
		return Arrays.stream( identifiers ).map( id -> new Object[] { id } ).toList();
	}

	private static class Fixture {
		private final TableMapping table = mock( TableMapping.class );
		private final EntityPersister persister = mock( EntityPersister.class );
		private final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		private final StatisticsImplementor statistics = mock( StatisticsImplementor.class );

		private Fixture() {
			final var factory = mock( SessionFactoryImplementor.class );
			final var identifierType = mock( Type.class );
			when( table.getTableName() ).thenReturn( "coverage_item" );
			when( table.isIdentifierTable() ).thenReturn( true );
			when( persister.getEntityPersister() ).thenReturn( persister );
			when( persister.getEntityName() ).thenReturn( "CoverageItem" );
			when( persister.getRootEntityName() ).thenReturn( "CoverageItem" );
			when( persister.getFactory() ).thenReturn( factory );
			when( persister.getIdentifierType() ).thenReturn( identifierType );
			when( identifierType.getTypeForEqualsHashCode() ).thenReturn( identifierType );
			when( identifierType.getHashCode( any(), same( factory ) ) )
					.thenAnswer( invocation -> normalized( invocation.getArgument( 0 ) ).hashCode() );
			when( identifierType.isEqual( any(), any(), same( factory ) ) )
					.thenAnswer( invocation -> Objects.equals(
							normalized( invocation.getArgument( 0 ) ), normalized( invocation.getArgument( 1 ) ) ) );
			when( identifierType.toLoggableString( any(), same( factory ) ) )
					.thenAnswer( invocation -> invocation.getArgument( 0 ).toString() );
			when( session.getFactory() ).thenReturn( factory );
			when( session.generateEntityKey( any(), same( persister ) ) )
					.thenAnswer( invocation -> new EntityKey( invocation.getArgument( 0 ), persister ) );
			when( factory.getStatistics() ).thenReturn( statistics );
			when( statistics.isStatisticsEnabled() ).thenReturn( true );
		}

		private List<EntityKey> keys(Object... identifiers) {
			return Arrays.stream( identifiers ).map( id -> new EntityKey( id, persister ) ).toList();
		}

		private void validate(List<EntityKey> expected, List<Object[]> acquired) {
			LockingHelper.validateAcquiredRows( table, persister, expected, acquired, WAIT_FOREVER, session );
		}

		private static Object normalized(Object value) {
			return value instanceof String string ? string.toLowerCase( Locale.ROOT ) : value;
		}
	}
}
