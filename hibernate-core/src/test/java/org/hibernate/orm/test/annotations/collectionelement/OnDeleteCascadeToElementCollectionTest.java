/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ExpectedExceptionExtension;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				OnDeleteCascadeToElementCollectionTest.Cascading.class,
				OnDeleteCascadeToElementCollectionTest.Ticket.class,
				OnDeleteCascadeToElementCollectionTest.NonCascading.class
		}
)
@SessionFactory(generateStatistics = true)
@ExtendWith(ExpectedExceptionExtension.class)
@JiraKey("HHH-4301")
public class OnDeleteCascadeToElementCollectionTest {
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
	public void testCascading(SessionFactoryScope scope) {
		var instance = new Cascading();

		scope.inTransaction(
				session -> {
					instance.labels = new HashSet<>( Set.of( "one", "two" ) );
					instance.tickets = new HashMap<>( Map.of(
							"t1", new Ticket( "t1-2398", LocalDate.of( 2023, 8, 26 ) ),
							"t2", new Ticket( "t2-23132", LocalDate.of( 2007, 9, 26 ) )
					) );
					session.persist( instance );
				}
		);

		scope.inTransaction(
				session -> {
					var deleted = session
							.createNativeQuery(
									"DELETE FROM Cascading WHERE id = " + instance.id )
							.executeUpdate();
					assertThat( deleted ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					var remained = session.createQuery(
									"select count(id) from Cascading" )
							.getSingleResult();
					assertThat( remained ).isEqualTo( 0L );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
	public void testCascadingDeleteUnloaded(SessionFactoryScope scope) {
		var instance = new Cascading();

		scope.inTransaction(
				session -> {
					instance.labels = new HashSet<>( Set.of( "one", "two" ) );
					instance.tickets = new HashMap<>( Map.of(
							"t1", new Ticket( "t1-2398", LocalDate.of( 2023, 8, 26 ) ),
							"t2", new Ticket( "t2-23132", LocalDate.of( 2007, 9, 26 ) )
					) );
					session.persist( instance );
				}
		);

		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				session -> {
					session.remove(session.getReference(instance));
				}
		);

		assertEquals( 0, statistics.getEntityLoadCount() );
		assertEquals( 1, statistics.getEntityDeleteCount() );
		assertEquals( 0, statistics.getCollectionLoadCount() );
		assertEquals( 0, statistics.getCollectionRemoveCount() );

		scope.inTransaction(
				session -> {
					assertEquals( 0L,
							session.createSelectionQuery( "from Cascading", Long.class )
									.getResultCount() );
					assertEquals( 0L,
							session.createNativeQuery( "select count(*) from Cascading_tickets", Long.class )
									.getSingleResult() );
					assertEquals( 0L,
							session.createNativeQuery( "select count(*) from Cascading_labels", Long.class )
									.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
	public void testCascadingDeleteLoaded(SessionFactoryScope scope) {
		var instance = new Cascading();

		scope.inTransaction(
				session -> {
					instance.labels = new HashSet<>( Set.of( "one", "two" ) );
					instance.tickets = new HashMap<>( Map.of(
							"t1", new Ticket( "t1-2398", LocalDate.of( 2023, 8, 26 ) ),
							"t2", new Ticket( "t2-23132", LocalDate.of( 2007, 9, 26 ) )
					) );
					session.persist( instance );
				}
		);

		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				session -> {
					Cascading entity = session.find(Cascading.class, instance.id);
					entity.labels.size();
					entity.tickets.size();
					session.remove( entity );
				}
		);

		assertEquals( 1, statistics.getEntityLoadCount() );
		assertEquals( 1, statistics.getEntityDeleteCount() );
		assertEquals( 2, statistics.getCollectionLoadCount() );
		assertEquals( 0, statistics.getCollectionRemoveCount() );

		scope.inTransaction(
				session -> {
					assertEquals( 0L,
							session.createSelectionQuery( "from Cascading", Long.class )
									.getResultCount() );
					assertEquals( 0L,
							session.createNativeQuery( "select count(*) from Cascading_tickets", Long.class )
									.getSingleResult() );
					assertEquals( 0L,
							session.createNativeQuery( "select count(*) from Cascading_labels", Long.class )
									.getSingleResult() );
				}
		);
	}

	@Test
	@ExpectedException(ConstraintViolationException.class)
	public void testNonCascading(SessionFactoryScope scope) {
		var instance = new NonCascading();

		scope.inTransaction(
				session -> {
					instance.labels = new HashSet<>( Set.of( "one", "two" ) );
					instance.tickets = new HashMap<>( Map.of(
							"t1", new Ticket( "t1-2398", LocalDate.of( 2023, 8, 26 ) ),
							"t2", new Ticket( "t2-23132", LocalDate.of( 2007, 9, 26 ) )
					) );
					session.persist( instance );
				}
		);

		scope.inTransaction(
				session -> {
					var deleted = session
							.createNativeQuery(
									"DELETE FROM NonCascading WHERE id = " + instance.id )
							.executeUpdate();
					assertThat( deleted ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Cascading")
	public static class Cascading {
		@Id
		@GeneratedValue
		public Long id;

		@ElementCollection
		@OnDelete(action = OnDeleteAction.CASCADE)
		public Set<String> labels;

		@ElementCollection
		@OnDelete(action = OnDeleteAction.CASCADE)
		public Map<String, Ticket> tickets;
	}

	@Embeddable
	public static class Ticket {
		public String serial;
		public LocalDate issuedOn;

		public Ticket() {
		}

		public Ticket(String serial, LocalDate issuedOn) {
			this.serial = serial;
			this.issuedOn = issuedOn;
		}
	}

	@Entity(name = "NonCascading")
	public static class NonCascading {
		@Id
		@GeneratedValue
		public Long id;

		@ElementCollection
		public Set<String> labels;

		@ElementCollection
		public Map<String, Ticket> tickets;

	}
}
