/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdConstraint;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the behavior of {@link NaturalIdConstraint}.
 *
 * <p>
 * By default, Hibernate generates the name of the unique constraint
 * used to enforce a {@link NaturalId} using the configured implicit
 * naming strategy.
 * </p>
 *
 * <p>
 * The {@link NaturalIdConstraint} annotation allows explicitly specifying
 * the name of this constraint at the entity level.
 * </p>
 *
 * <p>
 * This test verifies two scenarios:
 * </p>
 *
 * <ul>
 *     <li>
 *         When {@link NaturalIdConstraint} is present, the specified
 *         constraint name is used.
 *     </li>
 *     <li>
 *         When the annotation is absent, Hibernate falls back to the
 *         implicit naming strategy.
 *     </li>
 * </ul>
 *
 * @author Utsav Mehta
 */
@JiraKey("HHH-20000")
@ServiceRegistry
public class NaturalIdConstraintTest {

	@Test
	public void testNaturalIdUsesNameAttribute(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Person.class )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( Person.class.getName() )
				.getTable()
				.getUniqueKeys();

		assertEquals( 1, uniqueKeys.size() );

		var uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "PERSON_SSN", uniqueKey.getName() );
	}

	@Test
	public void testNaturalIdDefaultNaming(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( PersonWithoutConstraint.class )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( PersonWithoutConstraint.class.getName() )
				.getTable()
				.getUniqueKeys();

		assertEquals( 1, uniqueKeys.size() );

		var uniqueKey = uniqueKeys.values().iterator().next();

		// Default naming strategy should still be used
		assertFalse( uniqueKey.getName().isEmpty() );
	}

	@Entity(name = "Person")
	@NaturalIdConstraint(name = "PERSON_SSN")
	public static class Person {

		@Id
		private Long id;

		@NaturalId
		private String ssn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getSsn() {
			return ssn;
		}

		public void setSsn(String ssn) {
			this.ssn = ssn;
		}
	}

	@Entity(name = "PersonWithoutConstraint")
	public static class PersonWithoutConstraint {

		@Id
		Long id;

		@NaturalId
		String ssn;
	}
}
