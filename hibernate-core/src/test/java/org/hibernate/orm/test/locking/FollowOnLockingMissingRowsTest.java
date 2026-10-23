package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
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
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT;
import static jakarta.persistence.LockModeType.PESSIMISTIC_READ;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Deletes required rows after initial materialization but before follow-on
/// acquisition. Separate writer transactions make the acquisition gap deterministic.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FollowOnLockingMissingRowsTest.Item.class,
		FollowOnLockingMissingRowsTest.UnversionedItem.class
})
@SessionFactory(generateStatistics = true)
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21027")
class FollowOnLockingMissingRowsTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final int[] identifiers = { 1, 2, 30, 10, 20, 40 };
			for ( int position = 0; position < identifiers.length; position++ ) {
				session.persist( new Item( identifiers[position], position ) );
				session.persist( new UnversionedItem( identifiers[position], position ) );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Class<? extends BaseItem>> entityTypes() {
		return Stream.of( Item.class, UnversionedItem.class );
	}

	static Stream<Arguments> acquisitionCases() {
		return entityTypes().flatMap( type -> Stream.of( PESSIMISTIC_READ, PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT )
				.filter( mode -> type == Item.class || mode != PESSIMISTIC_FORCE_INCREMENT )
				.flatMap( mode -> Stream.of( false, true ).map( all -> arguments( type, mode, all ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("acquisitionCases")
	void missingRequiredRowsCauseStaleFailure(
			Class<? extends BaseItem> type,
			LockModeType mode,
			boolean deleteAll,
			SessionFactoryScope scope) {
		assertMissingRows( scope, type, mode, List.of( 1, 2 ), deleteAll ? List.of( 1, 2 ) : List.of( 2 ) );
	}

	@ParameterizedTest
	@MethodSource("entityTypes")
	void reportsSeveralMissingRowsInAcquisitionInputOrder(Class<? extends BaseItem> type, SessionFactoryScope scope) {
		assertMissingRows( scope, type, PESSIMISTIC_WRITE, List.of( 30, 10, 20, 40 ), List.of( 30, 20 ) );
	}

	@ParameterizedTest
	@MethodSource("entityTypes")
	void completeAcquisitionSucceeds(Class<? extends BaseItem> type, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var query = session.createQuery( "from " + entityName( type ) + " where id in :ids order by position", type )
					.setParameterList( "ids", List.of( 1, 2 ) )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			final var results = query.getResultList();
			assertThat( results ).extracting( item -> item.id ).containsExactly( 1, 2 );
			assertThat( results ).allSatisfy( item -> {
				assertThat( item.title ).isEqualTo( "initial-" + item.id );
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			} );
		} );
	}

	@Test
	void emptyInitialResultIsSuccessful(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var query = session.createQuery( "from MissingRowItem where id = -1", Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			assertThat( query.getResultList() ).isEmpty();
		} );
	}

	private void assertMissingRows(
			SessionFactoryScope scope,
			Class<? extends BaseItem> type,
			LockModeType mode,
			List<Integer> queryIds,
			List<Integer> missingIds) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( type );
		final String tableName = persister.getIdentifierTableDetails().getTableName();
		final var readerReference = new AtomicReference<SessionImplementor>();
		final List<String> statements = new ArrayList<>();
		final var snapshots = new LinkedHashMap<Integer, EntrySnapshot>();
		final UnaryOperator<String> inspector = sql -> {
			if ( !FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) {
				return sql;
			}
			statements.add( sql );
			if ( statements.size() == 2 ) {
				assertThat( sql ).contains( tableName );
				final var reader = readerReference.get();
				final var context = reader.getPersistenceContextInternal();
				for ( var id : queryIds ) {
					final var item = (BaseItem) context.getEntity( reader.generateEntityKey( id, persister ) );
					assertThat( item ).isNotNull();
					final var entry = context.getEntry( item );
					snapshots.put( id, new EntrySnapshot( item, entry, entry.getLockMode(), entry.getVersion() ) );
				}
				scope.inTransaction( writer -> {
					FollowOnLockingTestSupport.assertReadCommitted( writer );
					queryIds.stream().filter( id -> !missingIds.contains( id ) ).findFirst().ifPresent( survivor ->
							writer.createMutationQuery( "update " + entityName( type ) + " set title = 'changed' where id = :id" )
									.setParameter( "id", survivor ).executeUpdate() );
					assertThat( writer.createMutationQuery( "delete from " + entityName( type ) + " where id in :ids" )
							.setParameterList( "ids", missingIds ).executeUpdate() ).isEqualTo( missingIds.size() );
				} );
			}
			return sql;
		};
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var implementor = reader.unwrap( SessionImplementor.class );
			readerReference.set( implementor );
			final var transaction = reader.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( reader );
				final var graph = persister.createRootGraph( implementor );
				implementor.getLoadQueryInfluencers().applyEntityGraph( graph, GraphSemantic.LOAD );
				final var query = reader.createQuery( "from " + entityName( type ) + " where id in :ids order by position", type )
						.setParameterList( "ids", queryIds ).setLockMode( mode ).setFollowOnStrategy( Locking.FollowOn.FORCE );
				FollowOnLockingTestSupport.assertFollowOnPlan( implementor, query );
				final var failure = catchThrowableOfType( OptimisticLockException.class, query::getResultList );
				assertThat( failure ).hasCauseInstanceOf( StaleObjectStateException.class );
				final var stale = (StaleObjectStateException) failure.getCause();
				assertThat( stale.getEntityName() ).isEqualTo( persister.getEntityName() );
				assertThat( stale.getIdentifier() ).isEqualTo( missingIds.get( 0 ) );
				final var identifiers = new StringJoiner( ", ", "[", "]" );
				missingIds.forEach( id -> identifiers.add( id.toString() ) );
				assertThat( stale ).hasMessageStartingWith( "Follow-on locking failed for entity '" + persister.getEntityName()
						+ "' on table '" + tableName + "': missing identifiers " + identifiers );
				assertThat( stale.getSuppressed() ).isEmpty();
				assertThat( statements ).as( "initial selection and one batched acquisition, with no probes or increments" ).hasSize( 2 );
				assertThat( snapshots ).hasSize( queryIds.size() );
				final int titlePosition = persister.findAttributeMapping( "title" ).getStateArrayPosition();
				snapshots.values().forEach( snapshot -> {
					assertThat( snapshot.entity().title ).isEqualTo( "initial-" + snapshot.entity().id );
					assertThat( snapshot.entry().getLoadedState()[titlePosition] ).isEqualTo( snapshot.entity().title );
					assertThat( snapshot.entry().getLockMode() ).isEqualTo( snapshot.mode() );
					assertThat( snapshot.entry().getVersion() ).isEqualTo( snapshot.version() );
					if ( snapshot.entity() instanceof Item item ) {
						assertThat( item.version ).isZero();
					}
				} );
				assertThat( statistics.getOptimisticFailureCount() ).isEqualTo( 1 );
				assertThat( statistics.getEntityStatistics( persister.getEntityName() ).getOptimisticFailureCount() ).isEqualTo( 1 );
				assertThat( transaction.getRollbackOnly() ).isTrue();
				final var effectiveGraph = implementor.getLoadQueryInfluencers().getEffectiveEntityGraph();
				assertThat( effectiveGraph.getGraph() ).isSameAs( graph );
				assertThat( effectiveGraph.getSemantic() ).isEqualTo( GraphSemantic.LOAD );
				assertThat( implementor.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	private static String entityName(Class<? extends BaseItem> type) {
		return type == Item.class ? "MissingRowItem" : "UnversionedMissingRowItem";
	}

	private record EntrySnapshot(BaseItem entity, EntityEntry entry, LockMode mode, Object version) {}

	@MappedSuperclass
	static abstract class BaseItem {
		@Id int id;
		String title;
		@Column(name = "acquisition_position")
		int position;
		BaseItem() {}
		BaseItem(int id, int position) {
			this.id = id;
			this.title = "initial-" + id;
			this.position = position;
		}
	}

	@Entity(name = "MissingRowItem")
	@Table(name = "follow_on_missing_row_item")
	static class Item extends BaseItem {
		@Version int version;
		Item() {}
		Item(int id, int position) { super( id, position ); }
	}

	@Entity(name = "UnversionedMissingRowItem")
	@Table(name = "unversioned_follow_on_missing_row_item")
	static class UnversionedItem extends BaseItem {
		UnversionedItem() {}
		UnversionedItem(int id, int position) { super( id, position ); }
	}
}
