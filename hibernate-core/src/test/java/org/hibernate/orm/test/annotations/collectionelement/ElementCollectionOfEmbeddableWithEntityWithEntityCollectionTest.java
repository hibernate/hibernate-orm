/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.Event;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.Plan;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.SubPlan;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.annotations.CascadeType.ALL;
import static org.hibernate.annotations.FetchMode.SUBSELECT;

@DomainModel(
		annotatedClasses = {
				Plan.class,
				SubPlan.class,
				Event.class
		}
)
@SessionFactory( useCollectingStatementInspector = true )
@JiraKey(value = "HHH-15713")
public class ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Event event = new Event( 1, "event" );
					Event event2 = new Event( 2, "event2" );

					Event event3 = new Event( 3, "event3" );
					Event event4 = new Event( 4, "event4" );

					SubPlan subPlan = new SubPlan( 1 );
					subPlan.addEvent( event );
					subPlan.addEvent( event2 );

					SubPlan subPlan2 = new SubPlan( 2 );
					subPlan.addEvent( event3 );
					subPlan.addEvent( event4 );

					Transfer transfer = new Transfer( subPlan );
					Transfer transfer2 = new Transfer( subPlan2 );

					Plan plan = new Plan( 1 );
					plan.addTransfer( transfer );
					plan.addTransfer( transfer2  );

					session.persist( plan );
				}
		);
	}

	@Test
	public void testInitializeCollection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					Plan plan = session.get( Plan.class, 1 );
					Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					statementInspector.clear();
					assertThat( Hibernate.isInitialized( plan ) ).isTrue();
					assertThat( Hibernate.isInitialized( plan.getTransfers() ) ).isFalse();

					List<Transfer> transfers = plan.getTransfers();


					Transfer transfer = transfers.get( 0 );
					Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					statementInspector.clear();
					assertThat( Hibernate.isInitialized( transfer ) ).isTrue();
					assertThat( Hibernate.isPropertyInitialized( transfer, "subPlan" ) ).isTrue();

					Transfer transfer1 = transfers.get( 1 );
					Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 0 );

					SubPlan subPlan = transfer.getSubPlan();
					assertThat( Hibernate.isInitialized( subPlan.getEvents()) ).isFalse();

					SubPlan subPlan1 = transfer1.getSubPlan();
					assertThat( Hibernate.isInitialized( subPlan1.getEvents() ) ).isFalse();

					List<Event> events = subPlan.getEvents();
					Event event = events.get( 0 );
					assertThat( Hibernate.isInitialized( subPlan.getEvents()) ).isTrue();

					Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					assertThat(event).isNotNull();
					assertThat( Hibernate.isInitialized( event ) ).isTrue();

				}
		);
	}

	@Entity(name = "Plan")
	@Table(name = "PLAN_TABLE")
	public static class Plan {
		@Id
		public Integer id;

		private String name;

		@ElementCollection
		@OrderColumn(name = "position")
		@Cascade(ALL)
		@CollectionTable(name = "transfer", joinColumns = @JoinColumn(name = "plan_id"))
		public List<Transfer> transfers = new ArrayList<>();

		public Plan() {
		}

		public Plan(Integer id) {
			this.id = id;
		}

		public List<Transfer> getTransfers() {
			return transfers;
		}

		public void addTransfer(Transfer transfer) {
			this.transfers.add( transfer );
		}
	}

	@Embeddable
	public static class Transfer {
		@ManyToOne
		@JoinColumn(name = "subplan_id")
		@Cascade(ALL)
		public SubPlan subPlan;

		public Transfer() {
		}

		public Transfer(SubPlan subPlan) {
			this.subPlan = subPlan;
		}

		public SubPlan getSubPlan() {
			return subPlan;
		}
	}

	@Entity(name = "SubPlan")
	@Table(name = "SUBPLAN_TABLE")
	public static class SubPlan {
		@Id
		public Integer id;

		public String name;

		@ManyToMany
		@Fetch(SUBSELECT)
		@Cascade(ALL)
		public List<Event> events = new ArrayList<>();

		public SubPlan() {
		}

		public SubPlan(Integer id) {
			this.id = id;
		}

		public List<Event> getEvents() {
			return events;
		}

		public void addEvent(Event event) {
			this.events.add( event );
		}
	}

	@Entity(name = "Event")
	@Table(name = "EVENT_TABLE")
	public static class Event {
		@Id
		public Integer id;

		public String name;

		public Event() {
		}

		public Event(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
