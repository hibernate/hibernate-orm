package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.engine.spi.SelfDirtinessTracker;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayala Goldstein
 */

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class LazyToOneNoProxySetToNullTest extends BaseNonConfigCoreFunctionalTestCase {


	int employerInfoNoOrphanRemovalID = 1;
	int employerInfoWithOrphanRemovalID = 2;


	@Test
	@Ignore("Test is failing with ByteBuddy, but working with Javassist.")
	@TestForIssue(jiraKey = "HHH-13840")
	public void testSetToNullResetTheValue() {
		inTransaction(
				session -> {
					Employer employer1 = session.get( Employer.class, 1 );
					assertTrue( employer1 instanceof SelfDirtinessTracker );
					employer1.setEmployerInfoNoOrphanRemoval( null );
				}
		);
		inTransaction(
				session -> {
					Employer employer1 = session.get( Employer.class, 1 );
					assertNull( employer1.getEmployerInfoNoOrphanRemoval() );
					assertNotNull( session.get( EmployerInfo.class, employerInfoNoOrphanRemovalID ) );
				}
		);
	}


	//This test is failed for bytebuddy, works only in javaassist
	@Test
	@Ignore("Test is failing with ByteBuddy, but working with Javassist.")
	@TestForIssue(jiraKey = "HHH-12772")
	public void testLazyToOneOrphanRemovalIsWorking() {
		inTransaction(
				session -> {
					Employer employer = session.get( Employer.class, 1 );
					assertTrue( employer instanceof SelfDirtinessTracker );
					employer.setEmployerInfoWithOrphanRemoval( null );
				}
		);
		inTransaction(
				session -> {
					Employer employer = session.get( Employer.class, 1 );
					assertNull( employer.getEmployerInfoWithOrphanRemoval() );
					assertNull( session.get( EmployerInfo.class, employerInfoWithOrphanRemovalID ) );
					assertNotNull( session.get( EmployerInfo.class, employerInfoNoOrphanRemovalID ) );
				}
		);
	}


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EmployerInfo.class,
				Employer.class
		};
	}

	@Before
	public void setUpData() {
		inTransaction(
				session -> {
					final Employer employer = new Employer();
					employer.id = 1;

					final EmployerInfo employerInfo1 = new EmployerInfo();
					employerInfo1.id = employerInfoWithOrphanRemovalID;

					final EmployerInfo employerInfo2 = new EmployerInfo();
					employerInfo2.id = employerInfoNoOrphanRemovalID;

					employer.employerInfoWithOrphanRemoval = employerInfo1;
					employer.employerInfoNoOrphanRemoval = employerInfo2;

					session.persist( employer );
				}
		);
	}

	@After
	public void cleanupDate() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Employer" ).executeUpdate();
					session.createQuery( "delete from EmployerInfo" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Employer")
	public static class Employer {
		@Id
		private int id;

		@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinColumn(name = "info_id")
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private EmployerInfo employerInfoWithOrphanRemoval;

		@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
		@JoinColumn(name = "info2_id")
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private EmployerInfo employerInfoNoOrphanRemoval;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public EmployerInfo getEmployerInfoWithOrphanRemoval() {
			return employerInfoWithOrphanRemoval;
		}

		public void setEmployerInfoWithOrphanRemoval(EmployerInfo employerInfoWithOrphanRemoval) {
			this.employerInfoWithOrphanRemoval = employerInfoWithOrphanRemoval;
		}

		public EmployerInfo getEmployerInfoNoOrphanRemoval() {
			return employerInfoNoOrphanRemoval;
		}

		public void setEmployerInfoNoOrphanRemoval(EmployerInfo employerInfoNoOrphanRemoval) {
			this.employerInfoNoOrphanRemoval = employerInfoNoOrphanRemoval;
		}
	}

	@Entity(name = "EmployerInfo")
	public static class EmployerInfo {

		@Id
		private int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}
}