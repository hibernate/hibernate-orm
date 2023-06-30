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

import java.time.LocalDateTime;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import junit.framework.TestCase;

import static java.lang.System.out;
import static java.time.LocalDateTime.now;

/**
 * Illustrates the use of Hibernate native APIs, including the use
 * of org.hibernate.boot for configuration and bootstrap.
 * Configuration properties are sourced from hibernate.properties.
 *
 * @author Steve Ebersole
 */
public class HibernateIllustrationTest extends TestCase {
	private SessionFactory sessionFactory;

	@Override
	protected void setUp() {
		// A SessionFactory is set up once for an application!
		final StandardServiceRegistry registry =
				new StandardServiceRegistryBuilder()
						.build();
		try {
			sessionFactory =
					new MetadataSources(registry)
							.addAnnotatedClass(Event.class)
							.buildMetadata()
							.buildSessionFactory();
		}
		catch (Exception e) {
			// The registry would be destroyed by the SessionFactory, but we
			// had trouble building the SessionFactory so destroy it manually.
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	@Override
	protected void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	public void testBasicUsage() {
		// create a couple of events...
		sessionFactory.inTransaction(session -> {
			session.persist(new Event("Our very first event!", now()));
			session.persist(new Event("A follow up event", now()));
		});

		// now lets pull events from the database and list them
		sessionFactory.inTransaction(session -> {
			session.createSelectionQuery("from Event", Event.class).getResultList()
					.forEach(event -> out.println("Event (" + event.getDate() + ") : " + event.getTitle()));
		});
	}
}
