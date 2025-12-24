/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				OneToManySQLRestrictionJoinedInheritanceTest.Application.class,
				OneToManySQLRestrictionJoinedInheritanceTest.ApplicationProjectVersion.class,
				OneToManySQLRestrictionJoinedInheritanceTest.AbstractProjectVersion.class,
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-12016")
public class OneToManySQLRestrictionJoinedInheritanceTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Application app = new Application();
			app.setOid( 1L );

			ApplicationProjectVersion apv = new ApplicationProjectVersion();
			apv.setEffFromDtm( Instant.EPOCH );
			apv.setEffFromDtm( Instant.EPOCH.plus( 1L, ChronoUnit.DAYS ) );
			app.addApplicationProjectVersion( apv );
			app.addApplicationProjectVersion( new ApplicationProjectVersion() );

			entityManager.persist( app );
		} );
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Application appResult = entityManager.find( Application.class, 1L );

			assertNotNull( appResult );
			assertEquals( 1, appResult.getApplicationVersions().size() );
		} );
	}

	@Entity(name = "Application")
	public static class Application {

		@Id
		@Column(name = "APPLICATION_OID")
		Long oid;

		@OneToMany(mappedBy = "application", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("PROJ_VSN_EFF_TO_DTM is null")
		List<ApplicationProjectVersion> applicationVersions = new ArrayList<>();

		public void addApplicationProjectVersion(ApplicationProjectVersion apv) {
			apv.setApplication( this );

			if ( !applicationVersions.isEmpty() ) {
				apv.setVersionNumber(
						applicationVersions.get( applicationVersions.size() - 1 ).getVersionNumber() + 1 );
			}

			applicationVersions.add( apv );
		}


		public Long getOid() {
			return oid;
		}

		public void setOid(Long oid) {
			this.oid = oid;
		}


		public List<ApplicationProjectVersion> getApplicationVersions() {
			return applicationVersions;
		}

		public void setApplicationVersions(List<ApplicationProjectVersion> applicationVersions) {
			this.applicationVersions = applicationVersions;
		}
	}

	@Entity(name = "AbstractProjectVersion")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractProjectVersion {

		@Id
		@GeneratedValue
		Long oid;
		Instant effFromDtm;
		Instant effToDtm;

		public void makeCurrent() {
			setEffFromDtm( Instant.now() );
		}

		public Long getOid() {
			return oid;
		}

		public void setOid(Long oid) {
			this.oid = oid;
		}

		public Instant getEffFromDtm() {
			return effFromDtm;
		}

		public void setEffFromDtm(Instant effFromDtm) {
			this.effFromDtm = effFromDtm;
		}

		public Instant getEffToDtm() {
			return effToDtm;
		}

		public void setEffToDtm(Instant effToDtm) {
			this.effToDtm = effToDtm;
		}
	}

	@Entity(name = "ApplicationProjectVersion")
	public static class ApplicationProjectVersion extends AbstractProjectVersion {

		@ManyToOne
		@JoinColumn(name = "APPLICATION_OID", nullable = false)
		Application application;

		@Column(name = "APPLICATION_VERSION_NUM", nullable = false)
		Integer versionNumber = 1; // Version numbers start at 1 and count up for each new version in the given Application

		public ApplicationProjectVersion() {
			makeCurrent();
		}

		public ApplicationProjectVersion(Application application) {
			this();
			this.application = application;
		}

		public Application getApplication() {
			return application;
		}

		public void setApplication(Application application) {
			this.application = application;
		}

		public Integer getVersionNumber() {
			return versionNumber;
		}

		public void setVersionNumber(Integer versionNumber) {
			this.versionNumber = versionNumber;
		}

	}

}
