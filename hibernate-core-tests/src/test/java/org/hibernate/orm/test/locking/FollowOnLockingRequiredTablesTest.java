package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.TableDetails;

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

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Required secondary and joined-inheritance rows must be accounted for before
/// publishing acquisition success or applying that table's reread state.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FollowOnLockingRequiredTablesTest.SecondaryItem.class,
		FollowOnLockingRequiredTablesTest.JoinedRoot.class,
		FollowOnLockingRequiredTablesTest.JoinedItem.class
})
@SessionFactory
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21027")
class FollowOnLockingRequiredTablesTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			for ( int id = 1; id <= 2; id++ ) {
				session.persist( new SecondaryItem( id ) );
				session.persist( new JoinedItem( id ) );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> mappings() {
		return Stream.of( false, true ).flatMap( all -> Stream.of(
				arguments( SecondaryItem.class, "required_secondary_detail", all ),
				arguments( JoinedItem.class, "required_joined_detail", all ) ) );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void missingRequiredTableRowsFailBeforeApplyingTheirState(
			Class<?> entityType, String requiredTable, boolean deleteAll, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityType );
		final var detailMapping = (BasicValuedModelPart) persister.findAttributeMapping( "detail" );
		final var detailColumn = detailMapping.getSelectable( 0 );
		final String table = detailColumn.getContainingTableExpression();
		final var requiredTableDetails = new AtomicReference<TableDetails>();
		persister.forEachTableDetails( details -> {
			if ( details.getTableName().equals( table ) ) {
				requiredTableDetails.set( details );
			}
		} );
		assertThat( table ).containsIgnoringCase( requiredTable );
		assertThat( requiredTableDetails.get() ).as( "mapped required table" ).isNotNull();
		final String keyColumn = requiredTableDetails.get().getKeyDetails().getKeyColumn( 0 ).getColumnName();
		final var readerReference = new AtomicReference<SessionImplementor>();
		final List<String> statements = new ArrayList<>();
		final List<EntrySnapshot> snapshots = new ArrayList<>();
		final UnaryOperator<String> inspector = sql -> {
			if ( !FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) {
				return sql;
			}
			statements.add( sql );
			if ( statements.size() == 2 ) {
				final var reader = readerReference.get();
				for ( int id = 1; id <= 2; id++ ) {
					final var entity = reader.getPersistenceContextInternal().getEntity( reader.generateEntityKey( id, persister ) );
					assertThat( entity ).isNotNull();
					final var entry = reader.getPersistenceContextInternal().getEntry( entity );
					snapshots.add( new EntrySnapshot( entity, entry, entry.getLockMode() ) );
				}
				scope.inTransaction( writer -> {
					FollowOnLockingTestSupport.assertReadCommitted( writer );
					writer.doWork( connection -> {
						if ( !deleteAll ) {
							try ( var statement = connection.prepareStatement(
									"update " + table + " set " + detailColumn.getSelectionExpression() + " = ? where " + keyColumn + " = ?" ) ) {
								statement.setString( 1, "changed" );
								statement.setInt( 2, 1 );
								assertThat( statement.executeUpdate() ).isEqualTo( 1 );
							}
						}
						try ( var statement = connection.prepareStatement(
								"delete from " + table + (deleteAll ? "" : " where " + keyColumn + " = ?") ) ) {
							if ( !deleteAll ) {
								statement.setInt( 1, 2 );
							}
							assertThat( statement.executeUpdate() ).isEqualTo( deleteAll ? 2 : 1 );
						}
					} );
				} );
			}
			return sql;
		};
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			readerReference.set( reader.unwrap( SessionImplementor.class ) );
			final var transaction = reader.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( reader );
				final var query = reader.createQuery( "from " + persister.getEntityName() + " order by id", entityType )
						.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
				FollowOnLockingTestSupport.assertFollowOnPlan( readerReference.get(), query );
				final var failure = catchThrowableOfType( OptimisticLockException.class, query::getResultList );
				assertThat( failure ).hasCauseInstanceOf( StaleObjectStateException.class );
				final var stale = (StaleObjectStateException) failure.getCause();
				assertThat( stale.getEntityName() ).isEqualTo( persister.getEntityName() );
				assertThat( stale.getIdentifier() ).isEqualTo( deleteAll ? 1 : 2 );
				assertThat( stale ).hasMessageContaining( "on table '" + table + "': missing identifiers "
						+ (deleteAll ? "[1, 2]" : "[2]") );
				assertThat( statements ).hasSizeBetween( 2, 3 ).allSatisfy( sql -> assertThat( sql ).startsWith( "select " ) );
				assertThat( statements.get( statements.size() - 1 ) ).contains( table );
				assertThat( snapshots ).hasSize( 2 );
				final var detail = persister.findAttributeMapping( "detail" );
				snapshots.forEach( snapshot -> {
					assertThat( detail.getValue( snapshot.entity() ) ).isEqualTo( "initial" );
					assertThat( snapshot.entry().getLoadedState()[detail.getStateArrayPosition()] ).isEqualTo( "initial" );
					assertThat( snapshot.entry().getLockMode() ).isEqualTo( snapshot.mode() );
				} );
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	private record EntrySnapshot(Object entity, EntityEntry entry, LockMode mode) {}

	@Entity(name = "RequiredSecondaryItem")
	@Table(name = "required_secondary_item")
	@SecondaryTable(name = "required_secondary_detail", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
	@SecondaryRow(table = "required_secondary_detail", optional = false)
	static class SecondaryItem {
		@Id int id;
		@Version int version;
		@Column(table = "required_secondary_detail") String detail;
		SecondaryItem() {}
		SecondaryItem(int id) { this.id = id; detail = "initial"; }
	}

	@Entity(name = "RequiredJoinedRoot")
	@Table(name = "required_joined_root")
	@Inheritance(strategy = InheritanceType.JOINED)
	static abstract class JoinedRoot {
		@Id int id;
		@Version int version;
	}

	@Entity(name = "RequiredJoinedItem")
	@Table(name = "required_joined_detail")
	static class JoinedItem extends JoinedRoot {
		String detail;
		JoinedItem() {}
		JoinedItem(int id) { this.id = id; detail = "initial"; }
	}
}
