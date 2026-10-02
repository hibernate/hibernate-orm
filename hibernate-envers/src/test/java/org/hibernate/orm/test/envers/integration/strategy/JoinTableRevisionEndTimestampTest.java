package org.hibernate.orm.test.envers.integration.strategy;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies generated audit join tables store the timestamp which closes a collection revision.
///
/// @author Steve Ebersole
@JiraKey("HHH-11047")
@EnversTest(auditStrategies = ValidityAuditStrategy.class)
@Jpa(annotatedClasses = {
		JoinTableRevisionEndTimestampTest.Owner.class,
		JoinTableRevisionEndTimestampTest.Item.class,
		JoinTableRevisionEndTimestampTest.Revision.class
}, integrationSettings = @Setting(
		name = EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, value = "true"
))
public class JoinTableRevisionEndTimestampTest {
	@Test
	public void testJoinTableEndTimestamp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final var item = new Item();
			item.id = 1;
			entityManager.persist( item );
			final var owner = new Owner();
			owner.id = 1;
			owner.items.add( item );
			entityManager.persist( owner );
		} );

		scope.inTransaction( entityManager -> {
			final var rows = entityManager.createNativeQuery(
					"select REVEND, REVEND_TSTMP from owner_item_AUD", Object[].class
			).getResultList();
			assertThat( rows ).hasSize( 1 );
			assertThat( rows.get( 0 ) ).containsExactly( null, null );
		} );

		scope.inTransaction( entityManager -> entityManager.find( Owner.class, 1 ).items.clear() );

		scope.inTransaction( entityManager -> {
			final var rows = entityManager.unwrap( Session.class ).createNativeQuery(
					"select REV, REVEND, REVEND_TSTMP, REVTYPE from owner_item_AUD order by REV", Object[].class
			)
					.addScalar( "REV", Integer.class )
					.addScalar( "REVEND", Integer.class )
					.addScalar( "REVEND_TSTMP", Timestamp.class )
					.addScalar( "REVTYPE", Integer.class )
					.getResultList();
			assertThat( rows ).hasSize( 2 );
			final var closed = rows.get( 0 );
			final var latest = rows.get( 1 );
			final var endRevision = (Number) latest[0];
			assertThat( ((Number) closed[1]).intValue() ).isEqualTo( endRevision.intValue() );
			assertThat( closed[2] ).isInstanceOf( Timestamp.class );
			final var revision = AuditReaderFactory.get( entityManager ).findRevision( Revision.class, endRevision );
			assertThat( Math.abs( ((Timestamp) closed[2]).getTime() - revision.timestamp ) ).isLessThan( 1000L );
			assertThat( latest[1] ).isNull();
			assertThat( latest[2] ).isNull();
			assertThat( ((Number) latest[3]).intValue() ).isEqualTo( 2 );
		} );
	}

	@Entity(name = "TimestampOwner")
	@Audited
	public static class Owner {
		@Id
		Integer id;

		@ManyToMany
		@JoinTable(name = "owner_item")
		Set<Item> items = new HashSet<>();
	}

	@Entity(name = "TimestampItem")
	@Audited
	public static class Item {
		@Id
		Integer id;
	}

	@Entity(name = "JoinTableRevision")
	@RevisionEntity
	public static class Revision {
		@Id
		@GeneratedValue
		@RevisionNumber
		Integer id;

		@RevisionTimestamp
		long timestamp;
	}
}
