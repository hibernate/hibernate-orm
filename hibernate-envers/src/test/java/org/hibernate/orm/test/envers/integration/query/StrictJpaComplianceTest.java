/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;


import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@SuppressWarnings("unchecked")
@Jpa(
		annotatedClasses = {StrictJpaComplianceTest.Organization.class},
		integrationSettings = {
				@org.hibernate.testing.orm.junit.Setting(name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
		}
)
@EnversTest
public class StrictJpaComplianceTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReaderFactory.get( em ).getRevisions( Organization.class, 1 );
		} );
	}

	/**
	 * @author Madhumita Sadhukhan
	 */
	@Entity
	@Table(name = "ORG")
	public static class Organization {

		@Id
		@GeneratedValue
		@Audited
		private int id;

		@Audited
		@Column(name = "ORG_NAME")
		private String name;

		public Organization() {
		}

	}
}
