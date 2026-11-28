package org.hibernate.temporal.audit;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.audit.ModificationType.ADD;
import static org.hibernate.audit.ModificationType.DEL;
import static org.hibernate.audit.ModificationType.MOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// An audit row is a copy of the current row. It never holds a value which an
/// entity instance has, but which the statement writing the current row did
/// not write. The audit row which records a removal is a copy of the previous
/// audit row, since the current row held nothing else when it was deleted.
@JiraKey("HHH-21285")
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditRowCopyTest.Thing.class,
		AuditRowCopyTest.Defaulted.class,
		AuditRowCopyTest.Detailed.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets"))
class AuditRowCopyTest {

	@Entity(name = "CopiedThing")
	@Table(name = "copied_thing")
	@Audited
	@Audited.Table(name = "copied_thing_log")
	static class Thing {
		@Id int id = 1;
		String name;
		@Column(updatable = false) String code;

		Thing() {
		}

		Thing(String name, String code) {
			this.name = name;
			this.code = code;
		}
	}

	@Entity(name = "CopiedDefaulted")
	@Table(name = "copied_defaulted")
	@Audited
	@Audited.Table(name = "copied_defaulted_log")
	static class Defaulted {
		@Id int id = 1;
		String name;
		@Column(insertable = false) @ColumnDefault("'initial'") String status;
	}

	@Entity(name = "CopiedDetailed")
	@Table(name = "copied_detailed")
	@SecondaryTable(name = "copied_details")
	@Audited
	static class Detailed {
		@Id int id = 1;
		String name;
		@Column(table = "copied_details") String note;
	}

	private record Row(String name, String code, ModificationType type) {
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void changeToNonUpdatableAttributeOfManagedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> {
			var thing = session.find( Thing.class, 1 );
			thing.name = "b";
			// the update of the current row does not write this column
			thing.code = "c2";
		} );
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "b", "c1", MOD ) ), auditRows( scope ) );
	}

	@Test
	void statelessUpdateOfInstanceMissingNonUpdatableValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inStatelessTransaction( session -> session.update( new Thing( "b", null ) ) );
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "b", "c1", MOD ) ), auditRows( scope ) );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class,
			reason = "Spanner requires the default expression of a column to be parenthesized")
	void columnDefaultOfNonInsertableAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var defaulted = new Defaulted();
			defaulted.name = "a";
			session.persist( defaulted );
		} );
		scope.inSession( session -> assertEquals( List.of( "initial" ), session.createNativeQuery(
				"select status from copied_defaulted_log order by REV", String.class ).getResultList() ) );
	}

	@Test
	void removalRecordsTheLatestAuditRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> session.find( Thing.class, 1 ).name = "b" );
		scope.inTransaction( session -> session.find( Thing.class, 1 ).name = "c" );
		scope.inTransaction( session -> session.remove( session.find( Thing.class, 1 ) ) );
		assertEquals(
				List.of( new Row( "a", "c1", ADD ), new Row( "b", "c1", MOD ), new Row( "c", "c1", MOD ),
						new Row( "c", "c1", DEL ) ),
				auditRows( scope )
		);
	}

	@Test
	void removalDoesNotRecordAnUpdateOfItsOwnChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> {
			var thing = session.find( Thing.class, 1 );
			thing.name = "b";
			session.flush();
			session.remove( thing );
		} );
		// no changeset left the entity with the state written by the update
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "a", "c1", DEL ) ), auditRows( scope ) );
	}

	@Test
	void removalOfSeveralEntitiesInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Thing( "a", "c1" ) );
			var second = new Thing( "b", "c2" );
			second.id = 2;
			session.persist( second );
		} );
		scope.inTransaction( session -> session.find( Thing.class, 2 ).name = "b2" );
		scope.inTransaction( session -> {
			session.remove( session.find( Thing.class, 1 ) );
			session.remove( session.find( Thing.class, 2 ) );
		} );
		scope.inSession( session -> assertEquals( List.of( "a", "b2" ), session.createNativeQuery(
				"select name from copied_thing_log where REVTYPE=" + DEL.ordinal() + " order by id",
				String.class ).getResultList() ) );
	}

	@Test
	void removalOfRecreatedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> session.remove( session.find( Thing.class, 1 ) ) );
		scope.inTransaction( session -> session.persist( new Thing( "b", "c2" ) ) );
		scope.inTransaction( session -> session.remove( session.find( Thing.class, 1 ) ) );
		assertEquals(
				List.of( new Row( "a", "c1", ADD ), new Row( "a", "c1", DEL ), new Row( "b", "c2", ADD ),
						new Row( "b", "c2", DEL ) ),
				auditRows( scope )
		);
	}

	@Test
	void removalOfEntityWithNoAuditRow(SessionFactoryScope scope) {
		// the validity strategy does not accept a modification of an entity with no audit row
		assumeTrue( scope.getSessionFactory().getSessionFactoryOptions().getAuditStrategy() == AuditStrategy.DEFAULT );
		// the entity was not created by an audited session
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"insert into copied_thing (id, name, code) values (1, 'a', 'c1')" ).executeUpdate() );
		scope.inTransaction( session -> session.remove( session.find( Thing.class, 1 ) ) );
		scope.inSession( session -> assertNull( session.find( Thing.class, 1 ) ) );
		// there is no audit row to copy
		assertEquals( List.of(), auditRows( scope ) );
	}

	@Test
	void removalRecordsThePreviousRowOfEachTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var detailed = new Detailed();
			detailed.name = "a";
			detailed.note = "noted";
			session.persist( detailed );
		} );
		scope.inTransaction( session -> session.remove( session.find( Detailed.class, 1 ) ) );
		scope.inSession( session -> {
			assertEquals( List.of( "a", "a" ), session.createNativeQuery(
					"select name from copied_detailed_AUD order by REV", String.class ).getResultList() );
			assertEquals( List.of( "noted", "noted" ), session.createNativeQuery(
					"select note from copied_details_AUD order by REV", String.class ).getResultList() );
		} );
	}

	@Test
	void statelessRemovalRecordsTheStoredRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		// the given instance does not hold the stored state
		scope.inStatelessTransaction( session -> session.delete( new Thing( "stale", null ) ) );
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "a", "c1", DEL ) ), auditRows( scope ) );
	}

	@Test
	void removeAndPersistAgainInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> {
			session.remove( session.find( Thing.class, 1 ) );
			session.flush();
			session.persist( new Thing( "b", "c2" ) );
		} );
		// the net modification is recorded, from the current row
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "b", "c2", MOD ) ), auditRows( scope ) );
	}

	@Test
	void sessionSharingTheTransactionRemovesAndInsertsAgain(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> {
			try ( var child = session.statelessWithOptions().connection().open() ) {
				child.delete( child.get( Thing.class, 1 ) );
				child.insert( new Thing( "b", "c2" ) );
			}
		} );
		// the removal and the insert are merged in the changeset of the shared transaction
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "b", "c2", MOD ) ), auditRows( scope ) );
	}

	@Test
	void removePersistAndRemoveAgainInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Thing( "a", "c1" ) ) );
		scope.inTransaction( session -> {
			session.remove( session.find( Thing.class, 1 ) );
			session.flush();
			var thing = new Thing( "b", "c2" );
			session.persist( thing );
			session.flush();
			session.remove( thing );
		} );
		// no changeset left the entity with the state it was created again with
		assertEquals( List.of( new Row( "a", "c1", ADD ), new Row( "a", "c1", DEL ) ), auditRows( scope ) );
	}

	@Test
	void persistAndRemoveInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing( "a", "c1" );
			session.persist( thing );
			session.flush();
			session.remove( thing );
		} );
		scope.inSession( session -> assertNull( session.find( Thing.class, 1 ) ) );
		assertEquals( List.of(), auditRows( scope ) );
	}

	@Test
	void absentSecondaryRowIsRecordedAsNulls(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var detailed = new Detailed();
			detailed.name = "a";
			detailed.note = "noted";
			session.persist( detailed );
		} );
		scope.inTransaction( session -> {
			var detailed = session.find( Detailed.class, 1 );
			detailed.name = "b";
			// the row of the secondary table is deleted
			detailed.note = null;
		} );
		scope.inSession( session -> {
			final var notes = session.createNativeQuery(
					"select note from copied_details_AUD order by REV", String.class ).getResultList();
			assertEquals( 2, notes.size() );
			assertEquals( "noted", notes.get( 0 ) );
			assertNull( notes.get( 1 ) );
		} );
	}

	private static List<Row> auditRows(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
						"select name, code, REVTYPE from copied_thing_log order by REV", Object[].class )
				.getResultList().stream()
				.map( row -> new Row( (String) row[0], (String) row[1],
						ModificationType.values()[( (Number) row[2] ).intValue()] ) )
				.toList() );
	}
}
