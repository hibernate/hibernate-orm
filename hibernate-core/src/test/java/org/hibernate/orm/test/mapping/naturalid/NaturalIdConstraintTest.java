/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

		assertEquals( 1, uniqueKeys.size(), "Should have 1 unique key" );

		var uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "PERSON_SSN", uniqueKey.getName() );
	}

	@Test
	public void testCompositeNaturalIdUsesMatchingUniqueConstraint(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( CompositePerson.class )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( CompositePerson.class.getName() )
				.getTable()
				.getUniqueKeys();

		assertEquals( 1, uniqueKeys.size(), "Should have 1 unique key for composite natural-id" );

		var uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "PERSON_NATURAL_ID", uniqueKey.getName() );
	}

	@Test
	public void testCompositeNaturalIdIgnoresPartialConstraints(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( CompositePersonWithPartialConstraints.class )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( CompositePersonWithPartialConstraints.class.getName() )
				.getTable()
				.getUniqueKeys();

		assertEquals( 3, uniqueKeys.size() );

		// The natural id constraint covers both firstName and lastName, thus has 2 columns.
		// UK_FIRST and UK_LAST each have 1 column.
		var uniqueKey = uniqueKeys.values().stream()
				.filter( uk -> uk.getColumnSpan() == 2 )
				.findFirst()
				.get();

		// Should NOT use UK_FIRST or UK_LAST
		assertNotEquals( "UK_FIRST", uniqueKey.getName() );
		assertNotEquals( "UK_LAST", uniqueKey.getName() );
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
	@Table(uniqueConstraints = @UniqueConstraint(name = "PERSON_SSN", columnNames = "ssn"))
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
	@Entity(name = "CompositePerson")
	@Table(uniqueConstraints = @UniqueConstraint(
			name = "PERSON_NATURAL_ID",
			columnNames = {"firstName", "lastName"}
	))
	public static class CompositePerson {

		@Id
		Long id;

		@NaturalId
		String firstName;

		@NaturalId
		String lastName;
	}
	@Entity(name = "CompositePersonWithPartialConstraints")
	@Table(uniqueConstraints = {
			@UniqueConstraint(name = "UK_FIRST", columnNames = "firstName"),
			@UniqueConstraint(name = "UK_LAST", columnNames = "lastName")
	})
	public static class CompositePersonWithPartialConstraints {

		@Id
		Long id;

		@NaturalId
		String firstName;

		@NaturalId
		String lastName;
	}
}
