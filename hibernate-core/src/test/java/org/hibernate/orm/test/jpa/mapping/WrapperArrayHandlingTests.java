/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class WrapperArrayHandlingTests {
	@Test
	@ServiceRegistry(
			settings = @Setting( name = AvailableSettings.JPA_COMPLIANCE, value = "true" )
	)
	void testComplianceEnabled(ServiceRegistryScope scope) {
		try ( SessionFactory sessionFactory = buildSessionFactory( scope ) ) {
			// we expect this one to pass
		}
	}

	private SessionFactory buildSessionFactory(ServiceRegistryScope scope) {
		final MetadataSources metadataSources = new MetadataSources( scope.getRegistry() );
		final Metadata metadata = metadataSources.addAnnotatedClasses( TheEntity.class ).buildMetadata();
		return metadata.buildSessionFactory();
	}

	@Test
	@ServiceRegistry(
			settings = @Setting( name = AvailableSettings.JPA_COMPLIANCE, value = "false" )
	)
	void testComplianceDisabled(ServiceRegistryScope scope) {
		try ( SessionFactory sessionFactory = buildSessionFactory( scope ) ) {
			// however, this one should fall because DISALLOW is the default
			fail( "Expecting an exception due to DISALLOW" );
		}
		catch (Exception expected) {
		}
	}

	@Entity( name = "TheEntity" )
	@Table( name = "TheEntity" )
	public static class TheEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Basic
		private Character[] characters;
		@Basic
		private Byte[] bytes;

		protected TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Character[] getCharacters() {
			return characters;
		}

		public void setCharacters(Character[] characters) {
			this.characters = characters;
		}

		public Byte[] getBytes() {
			return bytes;
		}

		public void setBytes(Byte[] bytes) {
			this.bytes = bytes;
		}
	}
}
