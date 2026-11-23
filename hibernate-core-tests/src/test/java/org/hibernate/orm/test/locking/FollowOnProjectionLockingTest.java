package org.hibernate.orm.test.locking;

import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Structural follow-on coverage, including nested entity results and separate
/// query branches. Rejections must precede both selection and timeout commands.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FollowOnProjectionLockingTest.Item.class, FollowOnProjectionLockingTest.Target.class,
		FollowOnProjectionLockingTest.Vehicle.class, FollowOnProjectionLockingTest.Car.class,
		FollowOnProjectionLockingTest.Truck.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21026")
class FollowOnProjectionLockingTest {
	private static final String PREFIX = "Follow-on locking cannot acquire all requested locks from this result shape";
	private static final String ENTITY = "ProjectionLockItem";

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var target = new Target();
			target.id = 1;
			target.name = "target";
			session.persist( target );
			for ( int id = 1; id <= 2; id++ ) {
				final var item = new Item();
				item.id = id;
				item.name = "same";
				item.details = new Details();
				item.details.city = "city";
				item.target = id == 1 ? target : null;
				session.persist( item );
			}
			final var car = new Car();
			car.id = 1;
			session.persist( car );
			final var truck = new Truck();
			truck.id = 2;
			session.persist( truck );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, session.createQuery( "from " + ENTITY, Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> scalarCases() {
		return Stream.of( "i.name", "i.id", "i.id, i.name", "i.details", "upper(i.name)",
				"new list(i.id, i.name)", "new map(i.id as id, i.name as name)",
				"new " + ScalarView.class.getName() + "(i.id, i.name)" )
				.flatMap( selection -> Stream.of( false, true ).flatMap( empty ->
						Stream.of( Locking.FollowOn.FORCE, Locking.FollowOn.ALLOW )
								.map( strategy -> arguments( selection, empty, strategy ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("scalarCases")
	void rejectsScalarShapes(String selection, boolean empty, Locking.FollowOn strategy, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery(
				"select " + (strategy == Locking.FollowOn.ALLOW ? "distinct " : "") + selection
						+ " from " + ENTITY + " i" + (empty ? " where i.id < 0" : ""), Object.class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( strategy ), PREFIX );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "PESSIMISTIC_FORCE_INCREMENT" })
	void rejectsEveryPessimisticMode(LockModeType mode, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select i.name from " + ENTITY + " i", String.class )
				.setLockMode( mode ).setFollowOnStrategy( Locking.FollowOn.FORCE ), PREFIX );
		scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).version ).isZero() );
	}

	@ParameterizedTest
	@ValueSource(strings = { "count(i)", "1" })
	void rejectsUnidentifiableTargets(String selection, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select " + selection + " from " + ENTITY + " i", Object.class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "; no identifiable entity lock targets" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "scalar", "tuple", "constructor", "embedded" })
	void rejectsCriteriaProjections(String shape, SessionFactoryScope scope) {
		assertRejected( scope, session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Object.class );
			final var item = criteria.from( Item.class );
			criteria.select( switch ( shape ) {
				case "scalar" -> item.get( "name" );
				case "tuple" -> builder.array( item.get( "id" ), item.get( "name" ) );
				case "constructor" -> builder.construct( ScalarView.class, item.get( "id" ), item.get( "name" ) );
				case "embedded" -> item.get( "details" );
				default -> throw new AssertionError( shape );
			} );
			return session.createQuery( criteria ).setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
		}, PREFIX );
	}

	@ParameterizedTest
	@ValueSource(strings = { "i.name, i.target", "i, i.target.name", "new list(i.name, i.target)" })
	void rejectsPartialEntityCoverage(String selection, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select " + selection + " from " + ENTITY + " i", Object.class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "uncovered entity lock targets" );
	}

	@Test
	void distinguishesSelfJoinAliases(SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery(
				"select a, b.name from " + ENTITY + " a, " + ENTITY + " b where a.id = 1 and b.id = 2", Object[].class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "(b)" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "i", "i, i.name", "i, i.details", "new list(i, i.name)",
			"new map(i as item, i.name as name)" })
	void coveredResultsAcquireLocks(String selection, SessionFactoryScope scope) {
		assertCovered( scope, "select " + selection + " from " + ENTITY + " i", 2 );
	}

	@Test
	void nestedEntityConstructorAcquiresLocks(SessionFactoryScope scope) {
		assertCovered( scope, "select new " + EntityView.class.getName() + "(i, i.name) from " + ENTITY + " i", 2 );
	}

	@Test
	void bothSelectedEntitiesAreAcquired(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "select i, i.target from " + ENTITY + " i", Object[].class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 1 );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			assertThat( session.getCurrentLockMode( session.find( Target.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@Test
	void fetchedEntityCoveragePreservesScope(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "from " + ENTITY + " i left join fetch i.target", Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setLockScope( PessimisticLockScope.FETCHED )
					.setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 );
			assertThat( session.getCurrentLockMode( session.find( Target.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@Test
	void emptyAndNullableEntityResultsRemainValid(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "from " + ENTITY + " i where i.id < 0", Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).isEmpty();
			assertThat( session.createQuery( "select t from " + ENTITY + " i left join i.target t order by i.id", Target.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() )
					.hasSize( 2 ).containsNull();
		} );
	}

	@Test
	void preloadedStateDoesNotCoverScalarProjection(SessionFactoryScope scope) {
		assertRejected( scope, session -> {
			session.find( Item.class, 1 );
			return session.createQuery( "select i.name from " + ENTITY + " i", String.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
		}, PREFIX );
	}

	@Test
	void preloadedEntityResultsRemainValid(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertThat( session.createQuery( "from " + ENTITY + " i where i.id = 1", Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getSingleResult() ).isSameAs( item );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@Test
	void coveredSelfJoinAliasesRemainValid(SessionFactoryScope scope) {
		assertCovered( scope, "select a, b from " + ENTITY + " a, " + ENTITY + " b where a.id = 1 and b.id = 2", 1 );
	}

	@Test
	void groupedEntityAndCriteriaConstructorRemainValid(SessionFactoryScope scope) {
		assertCovered( scope, "select count(i), i from " + ENTITY
				+ " i group by i.id, i.name, i.details.city, i.target.id, i.version", 2 );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( EntityView.class );
			final var item = criteria.from( Item.class );
			criteria.select( builder.construct( EntityView.class, item, item.get( "name" ) ) );
			assertThat( session.createQuery( criteria ).setLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@Test
	void criteriaEntityArrayAndTupleRemainValid(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var array = builder.createQuery( Object[].class );
			final var arrayItem = array.from( Item.class );
			array.select( builder.array( arrayItem, arrayItem.get( "name" ) ) );
			assertThat( session.createQuery( array ).setLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 );
			final var tuple = builder.createTupleQuery();
			final var tupleItem = tuple.from( Item.class );
			tuple.select( builder.tuple( tupleItem, tupleItem.get( "name" ) ) );
			assertThat( session.createQuery( tuple ).setLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@Test
	void entityUnionAndPolymorphicResultsRemainValid(SessionFactoryScope scope) {
		assertCovered( scope, "select a from " + ENTITY + " a where a.id = 1 union all select b from " + ENTITY + " b where b.id = 2", 2 );
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> assertThat( session.createQuery( "from ProjectionLockVehicle", Vehicle.class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 2 ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"i, (select max(s.id) from ProjectionLockItem s), j.name",
			"i, (select s.name from ProjectionLockItem s where s.id = 2), j.name",
			"(select max(s.id) from ProjectionLockItem s), i, j.name"
	})
	void scalarAfterSubqueryStillRequiresEntityCoverage(String selection, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select " + selection
				+ " from " + ENTITY + " i, " + ENTITY + " j where i.id = 1 and j.id = 2", Object[].class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "Item(j)" );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"i, (select s.name from ProjectionLockItem s where s.id = 2), i.name",
			"(select s.name from ProjectionLockItem s where s.id = 2), i, i.name",
			"i, (select max(s.id) from ProjectionLockItem s), i.name"
	})
	void subqueryDoesNotAddOuterLockTargets(String selection, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			assertThat( session.createQuery( "select " + selection + " from " + ENTITY + " i where i.id = 1", Object[].class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( 1 );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries().stream()
					.filter( sql -> FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) ).hasSize( 2 );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 2 ) ) ).isEqualTo( LockMode.READ );
		} );
	}

	@Test
	void unionSubqueriesRetainEachBranchesLockTargets(SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery(
				"select a, (select max(s.id) from " + ENTITY + " s), a.name from " + ENTITY + " a where a.id = 1"
						+ " union all select b, (select max(t.id) from " + ENTITY + " t), c.name from "
						+ ENTITY + " b, " + ENTITY + " c where b.id = 2 and c.id = 1", Object[].class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "branch 2:" );
		assertCovered( scope, "select a, (select max(s.id) from " + ENTITY + " s) from " + ENTITY + " a where a.id = 1"
				+ " union all select b, (select max(t.id) from " + ENTITY + " t) from " + ENTITY + " b where b.id = 2", 2 );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"treat(v as ProjectionLockCar)",
			"v, treat(v as ProjectionLockCar)",
			"treat(v as ProjectionLockCar), v",
			"new list(treat(v as ProjectionLockCar))",
			"treat(v as ProjectionLockCar), treat(v as ProjectionLockCar).id"
	})
	void treatedEntityResultsSupplyTheirUnderlyingIdentity(String selection, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			final var query = session.createQuery( "select " + selection
					+ " from ProjectionLockVehicle v where v.id = 1", Object.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			scope.getCollectingStatementObserver().clear();
			assertThat( query.getResultList() ).hasSize( 1 );
			assertThat( session.getCurrentLockMode( session.find( Car.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Car.class );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries().stream()
					.filter( sql -> FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) ).hasSize( 2 );
		} );
	}

	@Test
	void criteriaTreatedEntitySuppliesItsUnderlyingIdentity(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Car.class );
			final var vehicle = criteria.from( Vehicle.class );
			criteria.select( builder.treat( vehicle, Car.class ) ).where( builder.equal( vehicle.get( "id" ), 1 ) );
			final var query = session.createQuery( criteria ).setLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			final var car = query.getSingleResult();
			assertThat( car.id ).isEqualTo( 1 );
			assertThat( session.getCurrentLockMode( car ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "a, treat(b as ProjectionLockCar).id", "treat(a as ProjectionLockCar), b.id" })
	void treatmentDoesNotCoverAnotherAliasWithTheSameIdentity(String selection, SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select " + selection
				+ " from ProjectionLockVehicle a, ProjectionLockVehicle b where a.id = 1 and b.id = 1", Object[].class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "Vehicle(b)" );
	}

	@Test
	void laterUnionBranchCannotLeaveATargetUncovered(SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select a, a.name from " + ENTITY + " a where a.id = 1"
				+ " union all select b, c.name from " + ENTITY + " b, " + ENTITY + " c where b.id = 2 and c.id = 1", Object[].class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE ), "branch 2:" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "scroll", "stream", "single", "stateless" })
	void everyConsumerRejectsBeforeSql(String consumer, SessionFactoryScope scope) {
		try ( var session = consumer.equals( "stateless" ) ? scope.getSessionFactory().openStatelessSession()
				: scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var query = session.createQuery( "select i.name from " + ENTITY + " i", String.class )
						.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
				scope.getCollectingStatementObserver().clear();
				final var assertion = assertThatThrownBy( () -> {
					switch ( consumer ) {
						case "scroll" -> { try ( var rows = query.scroll( ScrollMode.FORWARD_ONLY ) ) { rows.next(); } }
						case "stream" -> { try ( var rows = query.getResultStream() ) { rows.toList(); } }
						case "single" -> query.getSingleResult();
						case "stateless" -> query.getResultList();
						default -> throw new AssertionError( consumer );
					}
				} );
				if ( consumer.equals( "scroll" ) || consumer.equals( "stream" ) ) {
					assertion.isInstanceOf( IllegalQueryOperationException.class ).hasMessageStartingWith( PREFIX );
				}
				else {
					assertion.isInstanceOf( IllegalStateException.class ).hasCauseInstanceOf( IllegalQueryOperationException.class );
				}
				assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
				assertThat( ((SharedSessionContractImplementor) session).getJdbcCoordinator().getLogicalConnection()
						.getResourceRegistry().hasRegisteredResources() ).isFalse();
			}
			finally { if ( transaction.isActive() ) { transaction.rollback(); } }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void warmedScalarQueryCanRejectAndRecover(boolean initiallyLocked, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			final var query = session.createQuery( "select i.name from " + ENTITY + " i", String.class )
					.setHibernateLockMode( initiallyLocked ? LockMode.PESSIMISTIC_WRITE : LockMode.NONE )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			assertThat( query.getResultList() ).hasSize( 2 );
			query.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			scope.getCollectingStatementObserver().clear();
			assertThatThrownBy( query::getResultList ).isInstanceOf( IllegalStateException.class )
					.hasCauseInstanceOf( IllegalQueryOperationException.class );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
			query.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
					.setHibernateLockMode( initiallyLocked ? LockMode.PESSIMISTIC_WRITE : LockMode.NONE );
			assertThat( query.getResultList() ).hasSize( 2 );
		} );
	}

	@Test
	void skipLockedDiagnosticTakesPrecedence(SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select i.name from " + ENTITY + " i", String.class )
				.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeouts.SKIP_LOCKED ).setFollowOnStrategy( Locking.FollowOn.FORCE ),
				"SKIP_LOCKED is not supported with follow-on locking" );
	}

	@Test
	void rejectionPrecedesConnectionTimeoutActions(SessionFactoryScope scope) {
		assertRejected( scope, session -> session.createQuery( "select i.name from " + ENTITY + " i", String.class )
				.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeout.seconds( 1 ) )
				.setFollowOnStrategy( Locking.FollowOn.FORCE ), PREFIX );
	}

	@Test
	void ignoreRetainsItsOptOut(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery( "select distinct i.name from " + ENTITY + " i", String.class )
				.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.IGNORE ).getResultList() ).containsExactly( "same" ) );
	}

	private static void assertRejected(
			SessionFactoryScope scope, Function<SessionImplementor, SelectionQuery<?>> queryFactory, String diagnostic) {
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				final var query = queryFactory.apply( session );
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( query::getResultList ).isInstanceOf( IllegalStateException.class )
						.hasCauseInstanceOf( IllegalQueryOperationException.class ).satisfies( failure ->
								assertThat( failure.getCause() ).hasMessageContaining( diagnostic ) );
				assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
				assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
			}
			finally { if ( transaction.isActive() ) { transaction.rollback(); } }
		} );
	}

	private static void assertCovered(SessionFactoryScope scope, String hql, int size) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			assertThat( session.createQuery( hql, Object.class ).setLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE ).getResultList() ).hasSize( size );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 1 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			assertThat( session.getCurrentLockMode( session.find( Item.class, 2 ) ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries().stream()
					.filter( sql -> FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) ).hasSize( 2 );
		} );
	}

	public record ScalarView(int id, String name) {}
	public record EntityView(Item item, String name) {}

	@Embeddable
	static class Details { String city; }

	@Entity(name = ENTITY)
	@Table(name = "projection_lock_item")
	static class Item {
		@Id int id;
		@Version int version;
		String name;
		@Embedded Details details;
		@ManyToOne(fetch = FetchType.LAZY) Target target;
	}

	@Entity(name = "ProjectionLockTarget")
	@Table(name = "projection_lock_target")
	static class Target {
		@Id int id;
		@Version int version;
		String name;
	}

	@Entity(name = "ProjectionLockVehicle")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static abstract class Vehicle { @Id int id; }
	@Entity(name = "ProjectionLockCar")
	@Table(name = "projection_lock_car")
	static class Car extends Vehicle {}
	@Entity(name = "ProjectionLockTruck")
	@Table(name = "projection_lock_truck")
	static class Truck extends Vehicle {}
}
