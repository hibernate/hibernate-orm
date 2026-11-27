package org.hibernate.temporal.audit;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Generated;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// A value which the database generates when it writes the current row is
/// recorded in the audit log as it was stored, and is not generated again.
@JiraKey("HHH-21285")
@RequiresDialect(H2Dialect.class)
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditGeneratedValueTest.Token.class)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets"))
class AuditGeneratedValueTest {

	@Entity(name = "AuditedToken")
	@Table(name = "audited_token")
	@Audited
	@Audited.Table(name = "audited_token_log")
	static class Token {
		@Id int id = 1;
		String name;
		@Generated(event = { INSERT, UPDATE }, sql = "random_uuid()") UUID generated;
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void auditRowsHoldTheGeneratedValuesOfTheCurrentRow(SessionFactoryScope scope) {
		var inserted = scope.fromTransaction( session -> {
			var token = new Token();
			token.name = "a";
			session.persist( token );
			session.flush();
			return liveValue( session );
		} );
		var updated = scope.fromTransaction( session -> {
			session.find( Token.class, 1 ).name = "b";
			session.flush();
			return liveValue( session );
		} );
		assertNotEquals( inserted, updated );
		assertEquals( List.of( inserted, updated ), auditedValues( scope ) );
	}

	@Test
	void repeatedFlushesRecordTheLastGeneratedValue(SessionFactoryScope scope) {
		var last = scope.fromTransaction( session -> {
			var token = new Token();
			token.name = "a";
			session.persist( token );
			session.flush();
			token.name = "b";
			session.flush();
			return liveValue( session );
		} );
		assertEquals( List.of( last ), auditedValues( scope ) );
	}

	@Test
	void statelessWritesRecordTheGeneratedValues(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var token = new Token();
			token.name = "a";
			session.insert( token );
		} );
		var inserted = scope.fromSession( session -> liveValue( session ) );
		scope.inStatelessTransaction( session -> {
			var token = new Token();
			token.name = "b";
			session.update( token );
		} );
		var updated = scope.fromSession( session -> liveValue( session ) );
		assertNotEquals( inserted, updated );
		assertEquals( List.of( inserted, updated ), auditedValues( scope ) );
	}

	private static String liveValue(org.hibernate.Session session) {
		return session.createNativeQuery( "select cast(generated as varchar) from audited_token", String.class )
				.getSingleResult();
	}

	private static List<String> auditedValues(SessionFactoryScope scope) {
		return scope.fromSession( session -> session.createNativeQuery(
				"select cast(generated as varchar) from audited_token_log order by REV", String.class ).getResultList() );
	}
}
