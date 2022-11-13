/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.Event;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.Plan;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.SubPlan;
import org.hibernate.orm.test.annotations.collectionelement.ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest.Transfer;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.CascadeType.ALL;
import static org.hibernate.annotations.FetchMode.SUBSELECT;

@DomainModel(
		annotatedClasses = {
				Plan.class,
				Transfer.class,
				SubPlan.class,
				Event.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15713")
public class ElementCollectionOfEmbeddableWithEntityWithEntityCollectionTest {
	@Test
	public void initialize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var plan = new Plan();
					plan.id = 1;

					var subPlan = new SubPlan();
					subPlan.id = 2;

					var event = new Event();
					event.id = 3;
					event.name = "event";
					subPlan.events.add(event);

					var transfer = new Transfer();
					transfer.subPlan = subPlan;
					plan.transfers.add(transfer);

					session.persist(plan);
				}
		);

		scope.inTransaction(
				session -> {
					var plan = session.get(Plan.class, 1);
					Hibernate.initialize(plan.getTransfers().get(0).getSubPlan().getEvents());
				}
		);
	}

	@Entity
	public static class Plan {
		@Id
		public Integer id;

		@ElementCollection
		@OrderColumn(name = "position")
		@Cascade(ALL)
		@CollectionTable(name = "transfer", joinColumns = @JoinColumn(name = "plan_id"))
		public List<Transfer> transfers = new ArrayList<>();

		public List<Transfer> getTransfers() {
			return transfers;
		}
	}

	@Embeddable
	public static class Transfer {
		@ManyToOne
		@JoinColumn(name = "subplan_id")
		@Cascade(ALL)
		public SubPlan subPlan;

		public SubPlan getSubPlan() {
			return subPlan;
		}
	}

	@Entity
	public static class SubPlan {
		@Id
		public Integer id;

		@ManyToMany
		@Fetch(SUBSELECT)
		@Cascade(ALL)
		public List<Event> events = new ArrayList<>();

		public List<Event> getEvents() {
			return events;
		}
	}

	@Entity
	public static class Event {
		@Id
		public Integer id;

		public String name;
	}
}
