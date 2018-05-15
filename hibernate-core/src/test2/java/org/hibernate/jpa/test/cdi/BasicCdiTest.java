/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicCdiTest {

	private static int count;

	@Test
	@SuppressWarnings("unchecked")
	public void testIt() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( Monitor.class, EventQueue.class, Event.class );

		count = 0;

		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.applySetting( AvailableSettings.DELAY_CDI_ACCESS, "true" )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.build();

			final SessionFactoryImplementor sessionFactory;

			try {
				sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
						.addAnnotatedClass( MyEntity.class )
						.buildMetadata()
						.getSessionFactoryBuilder()
						.build();
			}
			catch ( Exception e ) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}

			try {
				inTransaction(
						sessionFactory,
						session -> session.persist( new MyEntity( 1 ) )
				);

				assertEquals( 1, count );

				inTransaction(
						sessionFactory,
						session -> {
							MyEntity it = session.find( MyEntity.class, 1 );
							assertNotNull( it );
						}
				);
			}
			finally {
				inTransaction(
						sessionFactory,
						session -> {
							session.createQuery( "delete MyEntity" ).executeUpdate();
						}
				);

				sessionFactory.close();
			}
		}
	}

	@Entity( name = "MyEntity" )
	@EntityListeners( Monitor.class )
	@Table(name = "my_entity")
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
				events = new ArrayList<>();
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

		@javax.inject.Inject
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
