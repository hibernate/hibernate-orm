/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

@TestForIssue(jiraKey = "test")
public class CompositeKeyJoinTest extends BaseEntityManagerFunctionalTestCase {
	private Company root_company;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Company.class,
				Event.class,
				EventDetail.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			Company company = new Company();
			company.setId(2L);
			entityManager.persist(company);
			root_company = company;

			Event event = new Event();
			event.setId(1L);
			event.setCompany(company);
			event.setEventType("old event type");
			entityManager.persist(event);

			EventDetail eventDetail = new EventDetail();
			eventDetail.setId(1L);
			eventDetail.setCompany(company);
			//eventDetail.setEvent(event);

			Set<EventDetail> eventDetailSet = new HashSet<>();
			eventDetailSet.add(eventDetail);
			event.setEventDetailSet(eventDetailSet);

			entityManager.persist(eventDetail);
			entityManager.persist(event);
		} );
	}


	@Test
	public void testEagerLoading() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<EventDetail> eventDetailList =  entityManager.createQuery(
					"select ed from EventDetail ed where ed.event.company  = :company", EventDetail.class )
					.setParameter("company", root_company)
					.getResultList();

			assertEquals(0, eventDetailList.size());

			//assertEquals( 2, stocks.size() );
			//assertEquals( "ABC", stocks.get( 0 ).getCode().getRefNumber() );

			//assertNull( stocks.get( 1 ).getCode() );

		} );
	}

	@Entity(name = "Event")
	@Access(AccessType.PROPERTY)
	public static class Event implements Serializable {

		private Long id = new Random().nextLong();
		private Company company;
		private Set<EventDetail> eventDetailSet;
		private String eventType;
		@Id
		@Column(name = "EVENT_SEQ")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@JoinColumn(name = "COMPANY_FK")
		@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY, optional = false)
		@Id
		public Company getCompany() {
			return company;
		}

		public void setCompany(Company company) {
			this.company = company;
		}

		@Column(name = "EVENT_TYPE")
		public String getEventType() {
			return eventType;
		}

		public void setEventType(String eventType) {
			this.eventType = eventType;
		}

		@OneToMany(cascade =  CascadeType.MERGE  , mappedBy = "event" )
		Set<EventDetail> getEventDetailSet()
		{
			return eventDetailSet;
		}

		public void setEventDetailSet(Set<EventDetail> eventDetailSet) {
			this.eventDetailSet = eventDetailSet;
		}
	}

	@Entity(name = "EventDetail")
	@Access(AccessType.PROPERTY)
	public static class EventDetail implements Serializable {

		private Long id;
		private Company company;
		private Event event;
		@Id
		@Column(name = "EVENT_DETAIL_SEQ")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}


		@JoinColumn(name = "COMPANY_FK")
		@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY, optional = true)
		@Id
		public Company getCompany() {
			return company;
		}

		public void setCompany(Company company) {
			this.company = company;
		}

		private Long eventSeq;

		@Column(name = "EVENT_FK")
		public Long getEventSeq() {
			return eventSeq;
		}

		public void setEventSeq(Long eventSeq) {
			this.eventSeq = eventSeq;
		}

		public void setEvent(Event event) {
			this.event = event;
			this.eventSeq = event == null ? null : event.getId();
		}

		@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY, optional = true)
		//@NotFound(action = NotFoundAction.IGNORE)
//    @JoinColumn(name = "EVENT_FK")
		@JoinColumns({
				@JoinColumn(name = "EVENT_FK", referencedColumnName = "EVENT_SEQ", insertable = false, updatable = false, nullable = true),
				@JoinColumn(name = "COMPANY_FK", referencedColumnName = "COMPANY_FK", insertable = false, updatable = false, nullable = true)
		})
//    @JoinColumnsOrFormulas(
//            value = {
//                    @JoinColumnOrFormula(column = @JoinColumn(referencedColumnName = "EVENT_SEQ", name = "EVENT_FK")),
//                    @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "COMPANY_FK", value = "COMPANY_FK"))
//            })
		public Event getEvent() {
			return event;
		}
	}

	@Entity(name = "Company")
	@Access(AccessType.PROPERTY)
	public static class Company implements Serializable {

		private Long id;

		@Id
		@Column(name = "COMPANY_SEQ")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

}
