/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12157")
public class TableGeneratorMultipleDefinitionTest extends BaseUnitTestCase {

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateGeneratorNamesDefinition() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, "true" )
				.build();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( TestEntity2.class )
					.addAnnotatedClass( TestEntity1.class )
					.buildMetadata();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TestEntity1")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5
	)
	public static class TestEntity1 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}

	@Entity(name = "TestEntity2")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier_2",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5
	)
	public static class TestEntity2 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}
}
