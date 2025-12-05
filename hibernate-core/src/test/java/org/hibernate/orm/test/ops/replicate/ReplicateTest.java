/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops.replicate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test trying to replicate HHH-11514
 *
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11514")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@FailureExpected(jiraKey = "HHH-11514")
@Jpa(
		annotatedClasses = {
				ReplicateTest.City.class
		}
)
public class ReplicateTest {

	@Test
	public void refreshTest(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			City city = new City();
			city.setId( 100L );
			city.setName( "Cluj-Napoca" );
			entityManager.unwrap( Session.class ).replicate( city, ReplicationMode.OVERWRITE );
		} );

		scope.inTransaction( entityManager -> {
			City city = entityManager.find( City.class, 100L );
			assertThat( city.getName() ).isEqualTo( "Cluj-Napoca" );
		} );
	}

	@Entity(name = "City")
	public static class City {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

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
	}
}
