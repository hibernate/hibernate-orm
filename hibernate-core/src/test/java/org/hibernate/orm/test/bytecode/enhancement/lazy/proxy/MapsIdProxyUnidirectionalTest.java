/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */

@JiraKey("HHH-13814")
@DomainModel(
		annotatedClasses = {
				MapsIdProxyUnidirectionalTest.EmployerInfo.class,
				MapsIdProxyUnidirectionalTest.Employer.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class MapsIdProxyUnidirectionalTest {

	@Test
	@JiraKey("HHH-13814")
	public void testBatchAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Statistics statistics = scope.getSessionFactory().getStatistics();
					statistics.clear();
					EmployerInfo employerInfo = session.get( EmployerInfo.class, 1 );

					assertEquals( 1, statistics.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( employerInfo, "employer" ) );
					assertFalse( Hibernate.isInitialized( employerInfo.employer ) );
					Hibernate.initialize( employerInfo.employer );

					assertThat( statistics.getEntityLoadCount() ).isEqualTo( 2L );
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 2L );
				}
		);
	}

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Employer employer = new Employer();
					employer.id =1;
					employer.name = "Employer #" + employer.id;
					final EmployerInfo employerInfo = new EmployerInfo();
					employerInfo.employer = employer;
					session.persist( employer );
					session.persist( employerInfo );
				}
		);
	}

	@AfterEach
	public void cleanupDate(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EmployerInfo")
	public static class EmployerInfo {
		@Id
		private int id;

		@MapsId
		@OneToOne(optional = false, fetch = FetchType.LAZY)
		private Employer employer;

	}

	@Entity(name = "Employer")
	public static class Employer {
		@Id
		private int id;

		private String name;
	}
}
