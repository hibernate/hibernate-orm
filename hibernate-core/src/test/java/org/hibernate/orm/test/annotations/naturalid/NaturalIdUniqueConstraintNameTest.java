/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

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

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-17132")
@ServiceRegistry
public class NaturalIdUniqueConstraintNameTest {
	private Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { City1.class, City2.class };
	}

	@Test
	public void testNaturalIdUsesUniqueConstraintName(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( getAnnotatedClasses() )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( City1.class.getName() )
				.getTable()
				.getUniqueKeys();

		// The unique key should not be duplicated for NaturalID + UniqueConstraint.
		assertEquals( 1, uniqueKeys.size() );

		// The unique key should use the name specified in UniqueConstraint.
		var uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "UK_zipCode_city", uniqueKey.getName() );
	}

	@Test
	public void testNaturalIdUsesExplicitColumns(ServiceRegistryScope registryScope) {
		var metadata = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( getAnnotatedClasses() )
				.buildMetadata();

		var uniqueKeys = metadata.getEntityBinding( City2.class.getName() )
				.getTable()
				.getUniqueKeys();

		// The unique key should not be duplicated for NaturalID + UniqueConstraint.
		assertEquals( 1, uniqueKeys.size() );

		// The unique key should use the name specified in UniqueConstraint.
		var uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "zipCode", uniqueKey.getColumns().get( 0 ).getName() );
		assertEquals( "city", uniqueKey.getColumns().get( 1 ).getName() );
	}

	@Entity(name = "City1")
	@Table(uniqueConstraints = @UniqueConstraint(name = "UK_zipCode_city", columnNames = { "zipCode", "city" }))
	public static class City1 {

		@Id
		private Long id;

		@NaturalId
		private String zipCode;
		@NaturalId
		private String city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getZipCode() {
			return zipCode;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}

	@Entity(name = "City2")
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "zipCode", "city" }))
	public static class City2 {

		@Id
		private Long id;

		@NaturalId
		private String zipCode;
		@NaturalId
		private String city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getZipCode() {
			return zipCode;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}
}
