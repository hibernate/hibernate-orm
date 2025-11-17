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
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12157")
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, value = "true")})
public class TableGeneratorMultipleDefinitionTest {

	@Test
	public void testDuplicateGeneratorNamesDefinition(ServiceRegistryScope scope) {
		Assertions.assertThrows( IllegalArgumentException.class, () -> {
					new MetadataSources( scope.getRegistry() )
							.addAnnotatedClass( TestEntity2.class )
							.addAnnotatedClass( TestEntity1.class )
							.buildMetadata();
				}
		);
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
