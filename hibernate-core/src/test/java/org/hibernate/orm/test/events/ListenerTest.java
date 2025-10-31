/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				ListenerTest.Person.class,
				ListenerTest.Customer.class
		}
)
@SessionFactory
public class ListenerTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@BeforeEach
	public void testLoadListener(SessionFactoryScope scope) {
		Serializable customerId = 1L;
		assertThatThrownBy( () ->
				scope.inTransaction( entityManager -> {
					//tag::events-interceptors-load-listener-example-part1[]
					EntityManagerFactory entityManagerFactory = /* ... */
							//end::events-interceptors-load-listener-example-part1[]
							entityManager.getEntityManagerFactory();
					//tag::events-interceptors-load-listener-example-part1[]
					entityManagerFactory.unwrap( SessionFactoryImplementor.class ).getEventListenerRegistry()
							.prependListeners( EventType.LOAD, new SecuredLoadEntityListener() );

					Customer customer = entityManager.find( Customer.class, customerId );
					//end::events-interceptors-load-listener-example-part1[]
				} ) ).isInstanceOf( SecurityException.class );
	}

	@Test
	public void testJPACallback(SessionFactoryScope scope) {
		Long personId = 1L;

		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.id = personId;
			person.name = "John Doe";
			person.dateOfBirth = Timestamp.valueOf( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ) );
			entityManager.persist( person );
		} );

		assertThatThrownBy( () ->
				scope.inTransaction( entityManager -> {
					Person person = entityManager.find( Person.class, personId );
					assertTrue( person.age > 0 );
				} ) ).isInstanceOf( SecurityException.class );
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Customer() {
		}

		public Customer(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	//tag::events-jpa-callbacks-example[]
	@Entity(name = "Person")
	@EntityListeners(LastUpdateListener.class)
	public static class Person {

		@Id
		private Long id;

		private String name;

		private Date dateOfBirth;

		@Transient
		private long age;

		private Date lastUpdate;

		public void setLastUpdate(Date lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		/**
		 * Set the transient property at load time based on a calculation.
		 * Note that a native Hibernate formula mapping is better for this purpose.
		 */
		@PostLoad
		public void calculateAge() {
			age = ChronoUnit.YEARS.between( LocalDateTime.ofInstant(
							Instant.ofEpochMilli( dateOfBirth.getTime() ), ZoneOffset.UTC ),
					LocalDateTime.now()
			);
		}
	}

	public static class LastUpdateListener {

		@PreUpdate
		@PrePersist
		public void setLastUpdate(Person p) {
			p.setLastUpdate( new Date() );
		}
	}
	//end::events-jpa-callbacks-example[]

	//tag::events-interceptors-load-listener-example-part2[]
	public static class SecuredLoadEntityListener implements LoadEventListener {
		// this is the single method defined by the LoadEventListener interface
		public void onLoad(LoadEvent event, LoadType loadType)
				throws HibernateException {
			if ( !Principal.isAuthorized( event.getEntityClassName(), event.getEntityId() ) ) {
				throw new SecurityException( "Unauthorized access" );
			}
		}
	}
	//end::events-interceptors-load-listener-example-part2[]

	public static class Principal {
		public static boolean isAuthorized(String clazz, Object id) {
			return false;
		}
	}

}
