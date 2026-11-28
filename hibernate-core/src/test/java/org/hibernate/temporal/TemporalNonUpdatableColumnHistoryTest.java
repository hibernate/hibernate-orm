package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A revision is a copy of the current row. It never holds a value which an
/// entity instance has, but which the statement writing the current row did
/// not write.
@JiraKey("HHH-21285")
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalNonUpdatableColumnHistoryTest.SUPPLIER)
})
@DomainModel(annotatedClasses = {
		TemporalNonUpdatableColumnHistoryTest.Thing.class,
		TemporalNonUpdatableColumnHistoryTest.Defaulted.class
})
@SessionFactory
class TemporalNonUpdatableColumnHistoryTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Entity(name = "NonUpdatableThing")
	@Table(name = "nonupd_thing")
	@Temporal
	static class Thing {
		@Id int id = 1;
		String name;
		@Column(updatable = false) String code;
	}

	@Entity(name = "DefaultedThing")
	@Table(name = "nonupd_defaulted")
	@Temporal
	static class Defaulted {
		@Id int id = 1;
		String name;
		@Column(insertable = false) @ColumnDefault("'initial'") String status;
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void changeToNonUpdatableAttributeOfManagedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing();
			thing.name = "a";
			thing.code = "c1";
			session.persist( thing );
		} );
		scope.inTransaction( session -> {
			var thing = session.find( Thing.class, 1 );
			thing.name = "b";
			// the update of the current row does not write this column
			thing.code = "c2";
		} );
		assertEquals( "c1", liveCode( scope ) );
		assertEquals( List.of( "a=c1", "b=c1" ), revisions( scope ) );
	}

	@Test
	void statelessUpdateOfInstanceMissingNonUpdatableValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing();
			thing.name = "a";
			thing.code = "c1";
			session.persist( thing );
		} );
		scope.inStatelessTransaction( session -> {
			var thing = new Thing();
			thing.name = "b";
			session.update( thing );
		} );
		assertEquals( "c1", liveCode( scope ) );
		assertEquals( List.of( "a=c1", "b=c1" ), revisions( scope ) );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class,
			reason = "Spanner requires the default expression of a column to be parenthesized")
	void columnDefaultOfNonInsertableAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Defaulted();
			thing.name = "a";
			session.persist( thing );
		} );
		scope.inTransaction( session -> session.find( Defaulted.class, 1 ).name = "b" );
		scope.inSession( session -> {
			assertEquals( "initial", session.createNativeQuery(
					"select status from nonupd_defaulted", String.class ).getSingleResult() );
			assertEquals( List.of( "a=initial", "b=initial" ),
					rows( session, "select name, status from nonupd_defaulted_history order by name" ) );
		} );
	}

	@Test
	void removeAndPersistAgainInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing();
			thing.name = "a";
			thing.code = "c1";
			session.persist( thing );
		} );
		scope.inTransaction( session -> {
			session.remove( session.find( Thing.class, 1 ) );
			session.flush();
			var thing = new Thing();
			thing.name = "b";
			thing.code = "c2";
			session.persist( thing );
		} );
		assertEquals( "c2", liveCode( scope ) );
		assertEquals( List.of( "a=c1", "b=c2" ), revisions( scope ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	@Test
	void persistAndRemoveInOneChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing();
			thing.name = "a";
			session.persist( thing );
			session.flush();
			session.remove( thing );
		} );
		scope.inSession( session -> assertNull( session.find( Thing.class, 1 ) ) );
		assertEquals( List.of(), revisions( scope ) );
	}

	private static String liveCode(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
				"select code from nonupd_thing", String.class ).getSingleResult() );
	}

	private static List<String> revisions(SessionFactoryScope scope) {
		return scope.fromSession( session -> rows( session, "select name, code from nonupd_thing_history order by name" ) );
	}

	private static List<String> rows(Session session, String sql) {
		return session.createNativeQuery( sql, Object[].class ).getResultList().stream()
				.map( row -> row[0] + "=" + row[1] )
				.toList();
	}

	private static long openRevisions(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
				"select count(*) from nonupd_thing_history where superseded is null", Long.class ).getSingleResult() );
	}
}
