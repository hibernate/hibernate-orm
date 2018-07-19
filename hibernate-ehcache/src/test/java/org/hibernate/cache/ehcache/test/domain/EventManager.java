/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.Query;
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
		Event event = (Event)session.load(Event.class, eventId);
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

		Long eventId = (Long)session.save(theEvent);

		session.getTransaction().commit();
		return eventId;
	}

	public Long createAndStorePerson(String firstName, String lastName) {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		Person person = new Person();
		person.setFirstname(firstName);
		person.setLastname(lastName);

		Long personId = (Long)session.save(person);

		session.getTransaction().commit();
		return personId;
	}

	public Long createAndStorePerson(Person person) {

		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		Long personId = (Long)session.save(person);

		session.getTransaction().commit();
		return personId;
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

		session.beginTransaction();

		Query query = session.createQuery("from Event ev where ev.organizer = :organizer");

		query.setCacheable(true);
		query.setEntity("organizer", organizer);
		List result = query.list();

		session.getTransaction().commit();

		return result;
	}

	/**
	 * Use a Criteria query - see FORGE-247
	 */
	public List listEventsWithCriteria() {
		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		List result = session.createCriteria(Event.class)
			.setCacheable(true)
			.list();

		session.getTransaction().commit();

		return result;
	}

	public void addPersonToEvent(Long personId, Long eventId) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.load(Person.class, personId);
		Event anEvent = (Event)session.load(Event.class, eventId);

		aPerson.getEvents().add(anEvent);

		session.getTransaction().commit();
	}

	public Long addPersonToAccount(Long personId, Account account) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.load(Person.class, personId);
		account.setPerson(aPerson);

		Long accountId = (Long)session.save(account);

		session.getTransaction().commit();
		return accountId;
	}

	public Account getAccount(Long accountId) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Account account = (Account)session.load(Account.class, accountId);

		session.getTransaction().commit();
		return account;
	}

	public void addEmailToPerson(Long personId, String emailAddress) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.load(Person.class, personId);

		// The getEmailAddresses() might trigger a lazy load of the collection
		aPerson.getEmailAddresses().add(emailAddress);

		session.getTransaction().commit();
	}

	public void addPhoneNumberToPerson(Long personId, PhoneNumber pN) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.load(Person.class, personId);
		pN.setPersonId(personId.longValue());
		aPerson.getPhoneNumbers().add(pN);

		session.getTransaction().commit();
	}

	public void addTalismanToPerson(Long personId, String talisman) {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		Person aPerson = (Person)session.load(Person.class, personId);
		aPerson.addTalisman(talisman);

		session.getTransaction().commit();
	}

	public Long createHolidayCalendar() {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();

		// delete all existing calendars
		List calendars = session.createQuery("from HolidayCalendar").setCacheable(true).list();
		for (ListIterator li = calendars.listIterator(); li.hasNext(); ) {
			session.delete(li.next());
		}

		HolidayCalendar calendar = new HolidayCalendar();
		calendar.init();

		Long calendarId = (Long)session.save(calendar);

		session.getTransaction().commit();
		return calendarId;
	}

	public HolidayCalendar getHolidayCalendar() {
		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();

		List calendars = session.createQuery("from HolidayCalendar").setCacheable(true).list();

		session.getTransaction().commit();

		return calendars.isEmpty() ? null : (HolidayCalendar)calendars.get(0);
	}
}
