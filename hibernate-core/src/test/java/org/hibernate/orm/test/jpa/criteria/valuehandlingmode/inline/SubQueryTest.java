/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Jpa(
		annotatedClasses = {
				SubQueryTest.Site.class,
				SubQueryTest.Task.class,
				SubQueryTest.Patient.class
		}
		, properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class SubQueryTest {
	private Set<Site> validSites;

	private Task taskWithoutPatient;
	private Task taskWithPatientWithoutSite;
	private Task taskWithPatient1WithValidSite1;
	private Task taskWithPatient2WithValidSite1;
	private Task taskWithPatient3WithValidSite2;
	private Task taskWithPatientWithInvalidSite;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Site validSite1 = new Site();
			final Site validSite2 = new Site();
			final Site invalidSite = new Site();

			entityManager.persist( validSite1 );
			entityManager.persist( validSite2 );
			entityManager.persist( invalidSite );

			validSites = new HashSet<>( Arrays.asList( validSite1, validSite2 ) );

			final Patient patientWithoutSite = new Patient();
			final Patient patient1WithValidSite1 = new Patient( validSite1 );
			final Patient patient2WithValidSite1 = new Patient( validSite1 );
			final Patient patient3WithValidSite2 = new Patient( validSite2 );
			final Patient patientWithInvalidSite = new Patient( invalidSite );

			entityManager.persist( patientWithoutSite );
			entityManager.persist( patient1WithValidSite1 );
			entityManager.persist( patient2WithValidSite1 );
			entityManager.persist( patient3WithValidSite2 );
			entityManager.persist( patientWithInvalidSite );

			taskWithoutPatient = new Task();
			taskWithoutPatient.description = "taskWithoutPatient";

			taskWithPatientWithoutSite = new Task( patientWithoutSite );
			taskWithPatientWithoutSite.description = "taskWithPatientWithoutSite";

			taskWithPatient1WithValidSite1 = new Task( patient1WithValidSite1 );
			taskWithPatient1WithValidSite1.description = "taskWithPatient1WithValidSite1";

			taskWithPatient2WithValidSite1 = new Task( patient2WithValidSite1 );
			taskWithPatient2WithValidSite1.description = "taskWithPatient2WithValidSite1";

			taskWithPatient3WithValidSite2 = new Task( patient3WithValidSite2 );
			taskWithPatient3WithValidSite2.description = "taskWithPatient3WithValidSite2";

			taskWithPatientWithInvalidSite = new Task( patientWithInvalidSite );
			taskWithPatientWithInvalidSite.description = "taskWithPatientWithInvalidSite";

			entityManager.persist( taskWithoutPatient );
			entityManager.persist( taskWithPatientWithoutSite );
			entityManager.persist( taskWithPatient1WithValidSite1 );
			entityManager.persist( taskWithPatient2WithValidSite1 );
			entityManager.persist( taskWithPatient3WithValidSite2 );
			entityManager.persist( taskWithPatientWithInvalidSite );
		} );
	}

	@Test
	public void testCorrelateSubQueryLeftJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Task> outerQuery = builder.createQuery( Task.class );
			final Root<Task> outerTask = outerQuery.from( Task.class );

			final Subquery<Task> subquery = outerQuery.subquery( Task.class );
			final Root<Task> subtask = subquery.correlate( outerTask );
			final EntityType<Task> taskEntityType = entityManager.getMetamodel().entity( Task.class );
			final EntityType<Patient> patientEntityType = entityManager.getMetamodel().entity( Patient.class );
			final From<Task, Patient> patient = subtask.join(
					taskEntityType.getSingularAttribute( "patient", Patient.class ),
					JoinType.LEFT
			);
			final From<Patient, Site> site = patient.join(
					patientEntityType.getSingularAttribute( "site", Site.class ),
					JoinType.LEFT
			);
			outerQuery.where(
					builder.exists(
							subquery.select( subtask )
									.where(
											builder.or(
													patient.isNull(),
													site.in( validSites )
											)
									)
					)
			);
			final List<Task> tasks = entityManager.createQuery( outerQuery ).getResultList();
			assertThat( new HashSet<>( tasks ), is( new HashSet<>( Arrays.asList(
					taskWithoutPatient,
					taskWithPatient1WithValidSite1,
					taskWithPatient2WithValidSite1,
					taskWithPatient3WithValidSite2
			) ) ) );

		} );
	}

	@Entity(name = "Task")
	@Table(name = "Task")
	public static class Task {

		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Patient patient;

		String description;

		public Task() {
		}

		public Task(Patient patient) {
			this.patient = patient;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Task task = (Task) o;
			return id.equals( task.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

		@Override
		public String toString() {
			return String.format( "Task(id: %d; description: %s)", id, description == null ? "null" : description );
		}
	}

	@Entity(name = "Patient")
	@Table(name = "Patient")
	public static class Patient {

		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Site site;

		public Patient() {
		}

		public Patient(Site site) {
			this.site = site;
		}

	}

	@Entity(name = "Site")
	@Table(name = "Site")
	public static class Site {

		@Id
		@GeneratedValue
		Long id;

		String name;

	}


}
