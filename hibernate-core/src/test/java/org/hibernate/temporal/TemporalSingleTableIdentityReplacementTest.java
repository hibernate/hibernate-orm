package org.hibernate.temporal;

import java.sql.SQLException;
import java.util.Locale;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-21221")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses = {
		TemporalSingleTableIdentityReplacementTest.TemporalIdentityItem.class,
		TemporalSingleTableIdentityReplacementTest.AssignedItem.class,
		TemporalSingleTableIdentityReplacementTest.OrdinaryIdentityItem.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = STATEMENT_BATCH_SIZE, value = "5"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets")
})
class TemporalSingleTableIdentityReplacementTest {
	@Test
	void retainsBothGeneratedIdentifiersWhenReplacedInOneFlush(SessionFactoryScope scope) {
		final var first = new TemporalIdentityItem( "first initial" );
		final var second = new TemporalIdentityItem( "second initial" );
		final var before = scope.fromTransaction( session -> {
			session.persist( first );
			session.persist( second );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		final Long firstId = first.id;
		final Long secondId = second.id;
		assertNotNull( firstId );
		assertNotNull( secondId );
		assertNotEquals( firstId, secondId );
		assertNotNull( before );

		scope.inTransaction( session -> {
			final var managedFirst = session.find( TemporalIdentityItem.class, firstId );
			final var managedSecond = session.find( TemporalIdentityItem.class, secondId );
			managedFirst.label = "first updated";
			managedSecond.label = "second updated";
			session.flush();
			assertEquals( firstId, managedFirst.id );
			assertEquals( secondId, managedSecond.id );
		} );

		scope.inSession( session -> {
			checkVersion( session.find( TemporalIdentityItem.class, firstId ), firstId, "first updated" );
			checkVersion( session.find( TemporalIdentityItem.class, secondId ), secondId, "second updated" );
		} );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession() ) {
			checkVersion( session.find( TemporalIdentityItem.class, firstId ), firstId, "first initial" );
			checkVersion( session.find( TemporalIdentityItem.class, secondId ), secondId, "second initial" );
		}
	}

	@Test
	@RequiresDialect(SybaseASEDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	void failedReplacementRestoresIdentityInsert(SessionFactoryScope scope) {
		final var item = new TemporalIdentityItem( "original" );
		scope.inTransaction( session -> session.persist( item ) );

		scope.inSession( outer -> outer.doWork( connection -> {
			try (var session = scope.getSessionFactory().withOptions().connection( connection ).openSession()) {
				final var transaction = session.beginTransaction();
				try {
					session.find( TemporalIdentityItem.class, item.id ).label = "rejected";
					final var error = assertThrows( PersistenceException.class, session::flush );
					assertTrue( error.getMessage().toLowerCase( Locale.ROOT ).contains( "ck_temporal_label" ), error::getMessage );
				}
				finally {
					transaction.rollback();
				}
			}
			// The physical connection must reject explicit identities again, even after a failed insert.
			try (var statement = connection.createStatement()) {
				final var error = assertThrows( SQLException.class, () -> statement.executeUpdate(
						"insert into TemporalIdentityReplacementItem (id,label,effective) values (1,'probe',0)" ) );
				assertTrue( error.getMessage().toLowerCase( Locale.ROOT ).contains( "identity_insert" ), error::getMessage );
			}
		} ) );
		scope.inSession( session -> checkVersion( session.find( TemporalIdentityItem.class, item.id ), item.id, "original" ) );
	}

	@Test
	void ordinaryIdentityInsertStillGeneratesIdentifier(SessionFactoryScope scope) {
		final var item = new OrdinaryIdentityItem();
		item.label = "ordinary";
		scope.inTransaction( session -> session.persist( item ) );
		assertNotNull( item.id );
		scope.inSession( session -> {
			final var current = session.find( OrdinaryIdentityItem.class, item.id );
			assertNotNull( current );
			assertEquals( item.id, current.id );
			assertEquals( "ordinary", current.label );
		} );
	}

	@Test
	void assignedIdStillSupportsTemporalReplacement(SessionFactoryScope scope) {
		final Long id = 100L;
		final var before = scope.fromTransaction( session -> {
			final var item = new AssignedItem();
			item.id = id;
			item.label = "assigned initial";
			session.persist( item );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		assertNotNull( before );
		scope.inTransaction( session -> session.find( AssignedItem.class, id ).label = "assigned updated" );
		scope.inSession( session -> {
			final var current = session.find( AssignedItem.class, id );
			assertNotNull( current );
			assertEquals( id, current.id );
			assertEquals( "assigned updated", current.label );
		} );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession() ) {
			final var historical = session.find( AssignedItem.class, id );
			assertNotNull( historical );
			assertEquals( id, historical.id );
			assertEquals( "assigned initial", historical.label );
		}
	}

	private static void checkVersion(TemporalIdentityItem item, Long id, String label) {
		assertNotNull( item );
		assertEquals( id, item.id );
		assertEquals( label, item.label );
	}

	@Entity(name = "TemporalIdentityReplacementItem")
	@Table(check = @CheckConstraint(name = "ck_temporal_label", constraint = "label <> 'rejected'"))
	@Temporal
	static class TemporalIdentityItem {
		@Id
		@GeneratedValue(strategy = IDENTITY)
		Long id;

		String label;

		TemporalIdentityItem() {
		}

		TemporalIdentityItem(String label) {
			this.label = label;
		}
	}

	@Entity(name = "TemporalAssignedReplacementItem")
	@Temporal
	static class AssignedItem {
		@Id
		Long id;

		String label;
	}

	@Entity(name = "OrdinaryIdentityReplacementItem")
	static class OrdinaryIdentityItem {
		@Id
		@GeneratedValue(strategy = IDENTITY)
		Long id;

		String label;
	}
}
