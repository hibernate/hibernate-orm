/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.enumeratedvalue;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class BadEnumeratedValueTests {

	@ServiceRegistry
	@Test
	void testMismatchedTypes(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Person2.class );

		try {
			metadataSources.buildMetadata();
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() ).startsWith( "@EnumeratedValue" );
		}
	}

	@Entity(name="Person2")
	@Table(name="persons2")
	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
	public static class Person2 {
		@Id
		private Integer id;
		private String name;
		@Enumerated
		private EnumeratedValueTests.Gender gender;
		@Enumerated(EnumType.STRING)
		private EnumeratedValueTests.Status status;

		public Person2() {
		}

		public Person2(Integer id, String name, EnumeratedValueTests.Gender gender, EnumeratedValueTests.Status status) {
			this.id = id;
			this.name = name;
			this.gender = gender;
			this.status = status;
		}
	}
}
