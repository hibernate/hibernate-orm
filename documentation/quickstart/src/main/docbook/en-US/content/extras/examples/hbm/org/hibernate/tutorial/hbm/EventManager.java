package org.hibernate.tutorial.hbm;

import org.hibernate.cfg.Configuration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Date;
import java.util.List;

public class EventManager {
    private final SessionFactory sessionFactory;

    public static void main(String[] args) {
        EventManager eventManager = new EventManager();

        if ( args[0].equals( "store" ) ) {
            eventManager.createAndStoreEvent( "My Event", new Date() );
        }
        else if (args[0].equals("list")) {
            List events = eventManager.listEvents();
            for (int i = 0; i < events.size(); i++) {
                Event theEvent = (Event) events.get(i);
                System.out.println(
                        "Event: " + theEvent.getTitle()
                            + " Time: " + theEvent.getDate()
                );
            }
        }

        eventManager.release();
    }

    public EventManager() {
        sessionFactory = new Configuration()
                .configure() // configures settings from hibernate.cfg.xml
                .buildSessionFactory();
    }

    public void release() {
        sessionFactory.close();
    }

    private void createAndStoreEvent(String title, Date theDate) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        Event theEvent = new Event();
        theEvent.setTitle( title );
        theEvent.setDate( theDate );
        session.save( theEvent );

        session.getTransaction().commit();
        session.close();
    }

    private List listEvents() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List result = session.createQuery("from Event").list();
        session.getTransaction().commit();
        session.close();
        return result;
    }
}