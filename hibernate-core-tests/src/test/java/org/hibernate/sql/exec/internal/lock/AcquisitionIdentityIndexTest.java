package org.hibernate.sql.exec.internal.lock;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.hibernate.AssertionFailure;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Acquisition matching and coverage must agree for internal indexes and extension maps.
///
/// @author Steve Ebersole
class AcquisitionIdentityIndexTest {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void duplicatesAndOrderingDoNotLeakBetweenAcquisitions(boolean fallback) {
		final var fixture = new Fixture();
		final var map = fixture.index( fallback, 1, 2, 3 );
		assertThat( fixture.resolve( map, fixture.keys( 1, 2, 1 ), 2, 1, 1 ) )
				.extracting( details -> details.key().getIdentifier() ).containsExactly( 2, 1, 1 );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 2, 3 ), 2, 2 ) )
				.isInstanceOf( StaleObjectStateException.class ).hasMessageContaining( "missing identifiers [3]" );
		assertThat( fixture.resolve( map, fixture.keys( 3 ), 3 ) ).hasSize( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void missingRowsPrecedeUnexpectedIdentities(boolean fallback) {
		final var fixture = new Fixture();
		final var map = fixture.index( fallback, 1, 2 );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 2, 1, 2 ), 9 ) )
				.isInstanceOf( StaleObjectStateException.class ).hasMessageContaining( "missing identifiers [2, 1]" );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 1 ), 1, 9 ) )
				.isInstanceOf( AssertionFailure.class ).hasMessageContaining( "no collected entity" );
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void optionalTableDoesNotRequireFullCoverage(boolean fallback) {
		final var fixture = new Fixture();
		when( fixture.table.isIdentifierTable() ).thenReturn( false );
		when( fixture.table.isOptional() ).thenReturn( true );
		final var map = fixture.index( fallback, 1, 2 );
		assertThat( fixture.resolve( map, fixture.keys( 1, 2 ) ) ).isEmpty();
		assertThat( fixture.resolve( map, fixture.keys( 1, 2 ), 2 ) ).hasSize( 1 );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 1, 2 ), 9 ) )
				.isInstanceOf( AssertionFailure.class );
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void usesHibernateIdentityAndRetainsFirstMissingSpelling(boolean fallback) {
		final var fixture = new Fixture();
		final var map = fixture.index( fallback, "Alpha", "Beta" );
		assertThat( fixture.resolve( map, fixture.keys( "ALPHA", "Beta" ), "BETA", "alpha" ) ).hasSize( 2 );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( "ALPHA", "Alpha", "Beta" ), "beta" ) )
				.isInstanceOf( StaleObjectStateException.class ).hasMessageContaining( "missing identifiers [ALPHA]" );
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void acquiredExpectedIdentityAbsentFromMapIsNotAMissingRow(boolean fallback) {
		final var fixture = new Fixture();
		final var map = fixture.index( fallback, 1 );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 1, 2 ), 1, 2 ) )
				.isInstanceOf( AssertionFailure.class ).hasMessageContaining( "no collected entity" );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 1, 2, 3 ), 1, 2 ) )
				.isInstanceOf( StaleObjectStateException.class ).hasMessageContaining( "missing identifiers [3]" );
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void replacementValuesDetermineAcquisitionIdentity(boolean throughEntry) {
		final var fixture = new Fixture();
		final var map = fixture.index( false, 1, 2 );
		final var second = map.remove( 2 );
		if ( throughEntry ) {
			map.entrySet().iterator().next().setValue( second );
		}
		else {
			map.put( 1, second );
		}
		assertThat( fixture.resolve( map, fixture.keys( 2 ), 2 ) ).containsExactly( second );
		assertThatThrownBy( () -> fixture.resolve( map, fixture.keys( 1 ), 1 ) )
				.isInstanceOf( AssertionFailure.class );
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

		private Map<Object, EntityDetails> index(boolean fallback, Object... identifiers) {
			final Map<Object, EntityDetails> map = fallback ? new java.util.HashMap<>() : new EntityDetailsMap( session, persister );
			for ( var key : keys( identifiers ) ) {
				final var details = new EntityDetails( key, null, null );
				if ( map instanceof EntityDetailsMap indexed ) {
					indexed.putEntityKey( key, details );
				}
				else {
					map.put( key.getIdentifier(), details );
				}
			}
			return map;
		}

		private List<EntityDetails> resolve(Map<Object, EntityDetails> map, List<EntityKey> expected, Object... acquired) {
			return TableLock.resolveAcquiredDetails( map, expected, rows( acquired ), table, persister, WAIT_FOREVER, session );
		}

		private static Object normalized(Object value) {
			return value instanceof String string ? string.toLowerCase( Locale.ROOT ) : value;
		}
	}
}
