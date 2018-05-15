/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-12157")
public class TableGeneratorMultipleDefinitionTest extends BaseUnitTestCase {

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateGeneratorNamesDefinition() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
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
