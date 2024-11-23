package org.hibernate.query.criteria.internal.hhh13058;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13058" )
public class HHH13058Test extends BaseEntityManagerFunctionalTestCase {

	private Set<Site> validSites;

	private Task taskWithoutPatient;
	private Task taskWithPatientWithoutSite;
	private Task taskWithPatient1WithValidSite1;
	private Task taskWithPatient2WithValidSite1;
	private Task taskWithPatient3WithValidSite2;
	private Task taskWithPatientWithInvalidSite;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Task.class,
				Patient.class,
				Site.class
		};
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testCorrelateSubQueryLeftJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Task> outerQuery = builder.createQuery( Task.class );
			final Root<Task> outerTask = outerQuery.from( Task.class );

			final Subquery<Task> subquery = outerQuery.subquery( Task.class );
			final Root<Task> subtask = subquery.correlate( outerTask );
			final From<Task, Patient> patient = subtask.join( Task_.patient, JoinType.LEFT );
			final From<Patient, Site> site = patient.join( Patient_.site, JoinType.LEFT );
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
}
