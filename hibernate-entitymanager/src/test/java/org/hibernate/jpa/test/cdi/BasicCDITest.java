/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.cdi;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicCDITest extends BaseCDIIntegrationTest {
	private static int count;

	@Override
	public Class[] getCdiBeans() {
		return new Class[] { EventQueue.class };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIt() {
		count = 0;

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new MyEntity( 1 ) );
		em.getTransaction().commit();
		em.close();

		assertEquals( 1, count );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( MyEntity.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	@Entity
	@EntityListeners( Monitor.class )
	public static class MyEntity {
		private Integer id;
		private String name;

		public MyEntity() {
		}

		public MyEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class EventQueue {
		private List<Event> events;

		public void addEvent(Event anEvent) {
			if ( events == null ) {
				events = new ArrayList<Event>();
			}
			events.add( anEvent );
			count++;
		}
	}

	public static class Event {
		private final String who;
		private final String what;
		private final String when;

		public Event(String who, String what, String when) {
			this.who = who;
			this.what = what;
			this.when = when;
		}

		public String getWho() {
			return who;
		}

		public String getWhat() {
			return what;
		}

		public String getWhen() {
			return when;
		}
	}

	public static class Monitor {
		private final EventQueue eventQueue;

		@Inject
		public Monitor(EventQueue eventQueue) {
			this.eventQueue = eventQueue;
		}

		@PrePersist
		public void onCreate(Object entity) {
			eventQueue.addEvent(
					new Event( entity.toString(), "created", now() )
			);
		}

		private String now() {
			return new SimpleDateFormat().format( new Date() );
		}
	}
}
