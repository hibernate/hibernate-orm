/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				BatchAndResultStreamTest.User.class,
				BatchAndResultStreamTest.Appointment.class
		}
)
@JiraKey( value = "HHH-16039")
public class BatchAndResultStreamTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					User user1 = new User();
					user1.setUsername( "user1" );

					entityManager.persist( user1 );

					Appointment appointment1 = new Appointment();
					appointment1.setOwner( user1 );
					appointment1.setCreatedBy( user1 );
					appointment1.setName( "Visit Gym" );
					appointment1.setDescription( "Exercise" );

					entityManager.persist( appointment1 );
				}
		);
	}

	@Test
	public void givenGetResultListCircularFetchImplShouldFetch(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "SELECT a FROM Appointment a WHERE a.owner.username = ?1" );
					query.setParameter( 1, "user1" );
					List<Appointment> result = query.getResultList();
					assertNotNull( result.get( 0 ).getOwner() );
				}
		);
	}

	@Test
	public void givenStreamIteratorCircularFetchImplShouldFetch(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "SELECT a FROM Appointment a WHERE a.owner.username = ?1" );
					query.setParameter( 1, "user1" );
					try (Stream<Appointment> stream = query.getResultStream()) {
						Appointment appointment = stream.iterator().next();
						assertNotNull( appointment.getOwner() );
					}
				}
		);

	}

	@Test
	public void givenStreamFindFirstCircularFetchImplShouldFetch(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "SELECT a FROM Appointment a WHERE a.owner.username = ?1" );
					query.setParameter( 1, "user1" );
					try (Stream<Appointment> stream = query.getResultStream()) {
						Appointment appointment = stream.findFirst().get();
						assertNotNull( appointment.getOwner() );
					}
				}
		);
	}

	@Entity(name = "Appointment")
	@Table(name = "APPT")
	public static class Appointment {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "apptIdSeq")
		private Long id;

		@Column(name = "NAME")
		private String name;

		@Column(name = "DESCRIPTION")
		private String description;

		@ManyToOne
		@JoinColumn(name = "OWNER_ID")
		private User owner;

		@ManyToOne
		@JoinColumn(name = "CREATED_BY")
		private User createdBy;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public User getOwner() {
			return owner;
		}

		public void setOwner(User owner) {
			this.owner = owner;
		}

		public User getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(User createdBy) {
			this.createdBy = createdBy;
		}
	}

	@Entity(name = "User")
	@Table(name = "APP_USER")
	@BatchSize(size = 10)
	public static class User {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "userIdSeq")
		private Long id;

		private String username;

		@OneToMany(mappedBy = "owner")
		private Set<Appointment> appointments;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Set<Appointment> getAppointments() {
			return appointments;
		}

		public void setAppointments(Set<Appointment> appointments) {
			this.appointments = appointments;
		}
	}
}
