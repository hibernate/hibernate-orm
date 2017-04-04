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
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@RunWith(Arquillian.class)
public class BasicCdiTest {
	@Deployment
	public static Archive<?> buildDeployment() {
		return ShrinkWrap.create( JavaArchive.class, "test.jar" )
				.addClass( MyEntity.class )
				.addClass( EventQueue.class )
				.addClass( Event.class )
				.addClass( Monitor.class )
				.addAsManifestResource( "jboss-deployment-structure.xml" )
				.addAsManifestResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsManifestResource( new StringAsset( persistenceXml().exportAsString() ), "persistence.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
				.createPersistenceUnit().name( "pu-cdi-basic" )
				.clazz( MyEntity.class.getName() )
				.excludeUnlistedClasses( true )
				.nonJtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties().createProperty().name( "jboss.as.jpa.providerModule" ).value( "org.hibernate:5.2" ).up().up()
				.getOrCreateProperties().createProperty().name( "hibernate.delay_cdi_access" ).value( "true" ).up().up()
				.getOrCreateProperties().createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up().up().up();
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Resource
	private UserTransaction utx;


	private static int count;

	@Test
	@SuppressWarnings("unchecked")
	public void testIt() throws Exception {
		count = 0;

		utx.begin();
		EntityManager em = emf.createEntityManager();
		em.persist( new MyEntity( 1 ) );
		utx.commit();

		assertEquals( 1, count );

		utx.begin();
		em = emf.createEntityManager();
		MyEntity it = em.find( MyEntity.class, 1 );
		assertNotNull( it );
		em.remove( it );
		utx.commit();
	}

	@Entity
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
