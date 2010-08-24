/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tutorial.annotations;

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