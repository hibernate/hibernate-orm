package org.hibernate.tutorial.web;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.domain.Person;
import org.hibernate.tutorial.util.HibernateUtil;

public class EventManager {

    public static void main(String[] args) {
        EventManager mgr = new EventManager();

        if (args[0].equals("store")) {
            mgr.createAndStoreEvent("My Event", new Date());
        }
        else if (args[0].equals("list")) {
            List events = mgr.listEvents();
            for (int i = 0; i < events.size(); i++) {
                Event theEvent = (Event) events.get(i);
                System.out.println("Event: " + theEvent.getTitle() +
                                   " Time: " + theEvent.getDate());
            }
        }
        else if (args[0].equals("addpersontoevent")) {
            Long eventId = mgr.createAndStoreEvent("My Event", new Date());
            Long personId = mgr.createAndStorePerson("Foo", "Bar");
            mgr.addPersonToEvent(personId, eventId);
            System.out.println("Added person " + personId + " to event " + eventId);
        }
        else if (args[0].equals("addemailtoperson")) {
            Long personId = mgr.createAndStorePerson("Foozy", "Beary");
            mgr.addEmailToPerson(personId, "foo@bar");
            mgr.addEmailToPerson(personId, "bar@foo");
            System.out.println("Added two email addresses (value typed objects) to person entity : " + personId);
        }

        HibernateUtil.getSessionFactory().close();
    }

    private Long createAndStoreEvent(String title, Date theDate) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Event theEvent = new Event();
        theEvent.setTitle(title);
        theEvent.setDate(theDate);

        session.save(theEvent);

        session.getTransaction().commit();

        return theEvent.getId();
    }

    private Long createAndStorePerson(String firstname, String lastname) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person thePerson = new Person();
        thePerson.setFirstname(firstname);
        thePerson.setLastname(lastname);

        session.save(thePerson);

        session.getTransaction().commit();

        return thePerson.getId();
    }


    private List listEvents() {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        List result = session.createQuery("from Event").list();

        session.getTransaction().commit();

        return result;
    }

    private void addPersonToEvent(Long personId, Long eventId) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = (Person) session
                .createQuery("select p from Person p left join fetch p.events where p.id = :pid")
                .setParameter("pid", personId)
                .uniqueResult(); // Eager fetch the collection so we can use it detached

        Event anEvent = (Event) session.load(Event.class, eventId);
        // If we want to handle it bidirectional and detached, we also need to load this
        // collection with an eager outer-join fetch, this time with Criteria and not HQL:
        /*
        Event anEvent = (Event) session
                .createCriteria(Event.class).setFetchMode("participants", FetchMode.JOIN)
                .add( Expression.eq("id", eventId) )
                .uniqueResult(); // Eager fetch the colleciton so we can use it detached
        */

        session.getTransaction().commit();

        // End of first unit of work

		aPerson.addToEvent( anEvent );
        // or bidirectional safety method, setting both sides: aPerson.addToEvent(anEvent);

        // Begin second unit of work

        Session session2 = HibernateUtil.getSessionFactory().getCurrentSession();
        session2.beginTransaction();

        session2.update(aPerson); // Reattachment of aPerson

        session2.getTransaction().commit();
    }

    private void addEmailToPerson(Long personId, String emailAddress) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = ( Person ) session.load(Person.class, personId);

        // The getEmailAddresses() might trigger a lazy load of the collection
        aPerson.getEmailAddresses().add(emailAddress);

        session.getTransaction().commit();
    }

}