/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class EventManager {

	private final SessionFactory sessionFactory;

	public EventManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public List listEmailsOfEvent(Long eventId) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		List emailList = new ArrayList();
		Event event = (Event)session.getReference(Event.class, eventId);
		for (Iterator it = event.getParticipants().iterator(); it.hasNext(); ) {
			Person person = (Person)it.next();
			emailList.addAll(person.getEmailAddresses());
		}

		session.getTransaction().commit();
		return emailList;
	}

	public Long createAndStoreEvent(String title, Person organizer, Date theDate) {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		Event theEvent = new Event();
		theEvent.setTitle(title);
		theEvent.setDate(theDate);
		theEvent.setOrganizer(organizer);

		session.persist( theEvent );

		session.getTransaction().commit();
		return theEvent.getId();
	}

	public Long createAndStorePerson(String firstName, String lastName) {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		Person person = new Person();
		person.setFirstname(firstName);
		person.setLastname(lastName);

		session.persist(person);

		session.getTransaction().commit();
		return person.getId();
	}

	public Long createAndStorePerson(Person person) {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		session.persist(person);

		session.getTransaction().commit();
		return person.getId();
	}

	public List listEvents() {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		List result = session.createQuery("from Event").setCacheable(true).list();

		session.getTransaction().commit();

		return result;
	}

	/**
	 * Call setEntity() on a cacheable query - see FORGE-265
	 */
	public List listEventsOfOrganizer(Person organizer) {

		Session session = sessionFactory.getCurrentSession();

		try {
			session.beginTransaction();

			Query query = session.createQuery( "from Event ev where ev.organizer = :organizer" );

			query.setCacheable( true );
			query.setParameter( "organizer", organizer );
			List result = query.list();

			session.getTransaction().commit();

			return result;
		}
		catch (Exception e){
			if(session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			throw e;
		}
	}

	/**
	 * Use a Criteria query - see FORGE-247
	 */
	public List listEventsWithCriteria() {
		Session session = sessionFactory.getCurrentSession();

		try {
			session.beginTransaction();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Event> criteria = criteriaBuilder.createQuery( Event.class );
			criteria.from( Event.class );

			List<Event> result = session.createQuery( criteria ).setCacheable( true ).list();
//			List result = session.createCriteria( Event.class )
//					.setCacheable( true )
//					.list();

			session.getTransaction().commit();

			return result;
		}
		catch (Exception e){
			if(session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			throw e;
		}
	}

	public void addPersonToEvent(Long personId, Long eventId) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.getReference(Person.class, personId);
		Event anEvent = (Event)session.getReference(Event.class, eventId);

		aPerson.getEvents().add(anEvent);

		session.getTransaction().commit();
	}

	public Long addPersonToAccount(Long personId, Account account) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.getReference(Person.class, personId);
		account.setPerson(aPerson);

		session.persist(account);

		session.getTransaction().commit();
		return account.getId();
	}

	public Account getAccount(Long accountId) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Account account = (Account)session.getReference(Account.class, accountId);

		session.getTransaction().commit();
		return account;
	}

	public void addEmailToPerson(Long personId, String emailAddress) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.getReference(Person.class, personId);

		// The getEmailAddresses() might trigger a lazy load of the collection
		aPerson.getEmailAddresses().add(emailAddress);

		session.getTransaction().commit();
	}

	public void addPhoneNumberToPerson(Long personId, PhoneNumber pN) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.getReference(Person.class, personId);
		pN.setPersonId(personId.longValue());
		aPerson.getPhoneNumbers().add(pN);

		session.getTransaction().commit();
	}

	public void addTalismanToPerson(Long personId, String talisman) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.getReference(Person.class, personId);
		aPerson.addTalisman(talisman);

		session.getTransaction().commit();
	}

	public Long createHolidayCalendar() {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		// delete all existing calendars
		List calendars = session.createQuery("from HolidayCalendar").setCacheable(true).list();
		for (ListIterator li = calendars.listIterator(); li.hasNext(); ) {
			session.remove(li.next());
		}

		HolidayCalendar calendar = new HolidayCalendar();
		calendar.init();

		session.persist(calendar);

		session.getTransaction().commit();
		return calendar.getId();
	}

	public HolidayCalendar getHolidayCalendar() {
		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		List calendars = session.createQuery("from HolidayCalendar").setCacheable(true).list();

		session.getTransaction().commit();

		return calendars.isEmpty() ? null : (HolidayCalendar)calendars.get(0);
	}
}
