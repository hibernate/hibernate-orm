package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.SharedSessionContract;
import org.hibernate.Timeouts;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Exercises pre-SQL skip-locked rejection without requiring contention or
/// database support for executing a skip-locked statement.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FollowOnSkipLockedRejectionTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21028")
class FollowOnSkipLockedRejectionTest {
	private static final String DIAGNOSTIC =
			"SKIP_LOCKED is not supported with follow-on locking; use direct locking for a supported query shape";

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			for ( var strategy : List.of( Locking.FollowOn.FORCE, Locking.FollowOn.ALLOW ) ) {
				for ( boolean empty : new boolean[] { false, true } ) {
					FollowOnLockingTestSupport.assertFollowOnPlan( session, query( session, strategy, empty ) );
				}
			}
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> listCases() {
		return Stream.of( Locking.FollowOn.FORCE, Locking.FollowOn.ALLOW )
				.flatMap( strategy -> Stream.of( false, true ).flatMap( legacy ->
						Stream.of( false, true ).map( empty -> arguments( strategy, legacy, empty ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("listCases")
	void rejectsBeforeListSql(Locking.FollowOn strategy, boolean legacy, boolean empty, SessionFactoryScope scope) {
		final List<String> statements = new ArrayList<>();
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector( statements ) ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				final var query = skip( query( session, strategy, empty ), legacy );
				statements.clear();
				assertRejected( query::getResultList, false );
				assertThat( statements ).isEmpty();
			}
			finally {
				transaction.rollback();
			}
		}
	}

	static Stream<Arguments> consumerCases() {
		return Stream.of( "scroll", "stream", "stateless" ).flatMap( consumer ->
				Stream.of( Locking.FollowOn.FORCE, Locking.FollowOn.ALLOW ).map( strategy -> arguments( consumer, strategy ) ) );
	}

	@ParameterizedTest
	@MethodSource("consumerCases")
	void rejectsBeforeConsumerSql(String consumer, Locking.FollowOn strategy, SessionFactoryScope scope) {
		final List<String> statements = new ArrayList<>();
		try ( SharedSessionContract session = consumer.equals( "stateless" )
				? scope.getSessionFactory().withStatelessOptions().statementInspector( inspector( statements ) ).openStatelessSession()
				: scope.getSessionFactory().withOptions().statementInspector( inspector( statements ) ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				final var query = skip( query( session, strategy, false ), false );
				statements.clear();
				assertRejected( () -> {
					switch ( consumer ) {
						case "scroll" -> {
							try ( var results = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
								results.next();
							}
						}
						case "stream" -> {
							try ( var results = query.getResultStream() ) {
								results.toList();
							}
						}
						case "stateless" -> query.getResultList();
						default -> throw new AssertionError( consumer );
					}
				}, !consumer.equals( "stateless" ) );
				assertThat( statements ).isEmpty();
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void warmedQueryRejectsAndRecovers(boolean legacy, SessionFactoryScope scope) {
		final List<String> statements = new ArrayList<>();
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector( statements ) ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				final var query = query( session, Locking.FollowOn.FORCE, false );
				assertThat( query.getResultList() ).extracting( item -> item.id ).containsExactly( 1, 2 );
				skip( query, legacy );
				statements.clear();
				assertRejected( query::getResultList, false );
				assertThat( statements ).isEmpty();
				query.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE ).setLockTimeout( Timeouts.WAIT_FOREVER );
				assertThat( query.getResultList() ).extracting( item -> item.id ).containsExactly( 1, 2 );
			}
			finally {
				transaction.rollback();
			}
		}
	}

	private static SelectionQuery<Item> query(SharedSessionContract session, Locking.FollowOn strategy, boolean empty) {
		return session.createQuery(
				(strategy == Locking.FollowOn.ALLOW ? "select distinct i from PortableSkipLockedItem i" : "from PortableSkipLockedItem i")
						+ (empty ? " where i.id < 0" : "") + " order by i.id", Item.class )
				.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( strategy );
	}

	private static SelectionQuery<Item> skip(SelectionQuery<Item> query, boolean legacy) {
		return legacy ? query.setHibernateLockMode( LockMode.UPGRADE_SKIPLOCKED ) : query.setLockTimeout( Timeouts.SKIP_LOCKED );
	}

	private static UnaryOperator<String> inspector(List<String> statements) {
		return sql -> { statements.add( sql ); return sql; };
	}

	private static void assertRejected(Runnable work, boolean unwrapped) {
		final var assertion = assertThatThrownBy( work::run );
		if ( unwrapped ) {
			assertion.isInstanceOf( IllegalQueryOperationException.class ).hasMessage( DIAGNOSTIC );
		}
		else {
			assertion.isInstanceOf( IllegalStateException.class ).hasCauseInstanceOf( IllegalQueryOperationException.class )
					.satisfies( failure -> assertThat( failure.getCause() ).hasMessage( DIAGNOSTIC ) );
		}
	}

	@Entity(name = "PortableSkipLockedItem")
	@Table(name = "portable_skip_locked_item")
	static class Item {
		@Id int id;
		Item() {}
		Item(int id) { this.id = id; }
	}
}
