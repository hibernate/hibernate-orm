package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The history and audit work pending for a transaction is not part of the
/// serialized state of a session.
@JiraKey("HHH-21285")
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalSessionSerializationTest.SUPPLIER)
})
@DomainModel(annotatedClasses = {
		TemporalSessionSerializationTest.Versioned.class,
		TemporalSessionSerializationTest.Logged.class
})
@SessionFactory
class TemporalSessionSerializationTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Entity(name = "SerializedVersioned")
	@Table(name = "serialized_versioned")
	@Temporal
	static class Versioned {
		@Id int id = 1;
		String name;
	}

	@Entity(name = "SerializedLogged")
	@Table(name = "serialized_logged")
	@Audited
	static class Logged {
		@Id int id = 1;
		String name;
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void sessionIsSerializableAfterWritingHistoryAndAuditRows(SessionFactoryScope scope) {
		final byte[] serialized;
		try ( var session = scope.getSessionFactory().openSession() ) {
			session.beginTransaction();
			var versioned = new Versioned();
			versioned.name = "a";
			session.persist( versioned );
			var logged = new Logged();
			logged.name = "a";
			session.persist( logged );
			session.getTransaction().commit();
			session.clear();
			serialized = SerializationHelper.serialize( session );
		}
		try ( var session = (Session) SerializationHelper.deserialize( serialized ) ) {
			session.beginTransaction();
			session.find( Versioned.class, 1 ).name = "b";
			session.find( Logged.class, 1 ).name = "b";
			session.getTransaction().commit();
		}
		scope.inSession( session -> {
			assertEquals( List.of( "a", "b" ), session.createNativeQuery(
					"select name from serialized_versioned_history order by effective", String.class ).getResultList() );
			assertEquals( List.of( "a", "b" ), session.createNativeQuery(
					"select name from serialized_logged_AUD order by REV", String.class ).getResultList() );
		} );
	}
}
