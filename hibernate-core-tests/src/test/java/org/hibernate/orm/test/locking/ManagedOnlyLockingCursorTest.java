package org.hibernate.orm.test.locking;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Acquisition callbacks must run even when every cursor result is already managed.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = ManagedOnlyLockingCursorTest.Item.class)
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
class ManagedOnlyLockingCursorTest {
	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Item() ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> modesAndConsumption() {
		return Stream.of( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE, LockMode.PESSIMISTIC_FORCE_INCREMENT )
				.flatMap( mode -> Stream.of( false, true ).map( scroll -> Arguments.of( mode, scroll ) ) );
	}

	@ParameterizedTest
	@MethodSource("modesAndConsumption")
	void directExtended(LockMode mode, boolean scroll, SessionFactoryScope scope) {
		exercise( mode, scroll, false, PessimisticLockScope.EXTENDED, false, scope );
	}

	@ParameterizedTest
	@MethodSource("modesAndConsumption")
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void followOnExtended(LockMode mode, boolean scroll, SessionFactoryScope scope) {
		exercise( mode, scroll, true, PessimisticLockScope.EXTENDED, false, scope );
	}

	@ParameterizedTest
	@MethodSource("modesAndConsumption")
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void followOnNormal(LockMode mode, boolean scroll, SessionFactoryScope scope) {
		exercise( mode, scroll, true, PessimisticLockScope.NORMAL, false, scope );
	}

	@ParameterizedTest
	@MethodSource("modesAndConsumption")
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void followOnPhysicallyProtectsManagedResult(LockMode mode, boolean scroll, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		exercise( mode, scroll, true, PessimisticLockScope.EXTENDED, true, scope );
	}

	private void exercise(
			LockMode mode, boolean scroll, boolean followOn, PessimisticLockScope lockScope,
			boolean physical, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var managed = session.find( Item.class, 1 );
			assertThat( managed.postLoads ).isEqualTo( 1 );
			final var query = session.createQuery( "from ManagedCursorItem where id = 1", Item.class )
					.setHibernateLockMode( mode ).setLockScope( lockScope )
					.setFollowOnStrategy( followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
			if ( followOn ) {
				FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			}
			scope.getCollectingStatementObserver().clear();
			if ( scroll ) {
				try ( var cursor = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
					assertThat( cursor.next() ).isTrue();
					assertThat( cursor.get() ).isSameAs( managed );
					assertAcquired( session, managed, mode, physical, scope );
					assertThat( cursor.next() ).isFalse();
				}
			}
			else {
				try ( var results = query.getResultStream() ) {
					assertThat( results.findFirst().orElseThrow() ).isSameAs( managed );
					assertAcquired( session, managed, mode, physical, scope );
				}
			}
			assertThat( managed.postLoads ).isEqualTo( 1 );
			if ( lockScope == PessimisticLockScope.EXTENDED ) {
				assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
						.anyMatch( sql -> sql.contains( "from managed_cursor_tags " ) );
			}
			else {
				assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
						.noneMatch( sql -> sql.contains( "from managed_cursor_tags " ) );
			}
		} );
	}

	private void assertAcquired(Session session, Item item, LockMode mode, boolean physical, SessionFactoryScope scope) {
		assertThat( session.getCurrentLockMode( item ) ).isEqualTo( mode );
		assertThat( item.version ).isEqualTo( mode == LockMode.PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 );
		if ( physical ) {
			for ( var table : new String[] { "managed_cursor_item", "managed_cursor_tags" } ) {
				final var key = table.endsWith( "tags" ) ? "owner_id" : "id";
				final var rows = scope.fromTransaction( contender -> contender.createNativeQuery(
						FollowOnLockingTestSupport.writeProtectionProbeSql( (SharedSessionContractImplementor) contender,
								"select 1 from " + table + " t where " + key + " = 1", Map.of( "t", new String[] { key } ) ),
						Integer.class ).getResultList() );
				assertThat( rows ).as( table + " protection" ).isEmpty();
			}
		}
	}

	@Entity(name = "ManagedCursorItem")
	@Table(name = "managed_cursor_item")
	static class Item {
		@Id int id = 1;
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "managed_cursor_tags", joinColumns = @JoinColumn(name = "owner_id"))
		Set<String> tags = new HashSet<>( Set.of( "tag" ) );
		@jakarta.persistence.Transient int postLoads;

		@PostLoad
		void loaded() {
			postLoads++;
		}
	}
}
