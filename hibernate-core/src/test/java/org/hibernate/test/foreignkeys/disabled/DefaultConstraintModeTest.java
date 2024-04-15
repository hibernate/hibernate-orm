/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.foreignkeys.disabled;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.stream.StreamSupport;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Table;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Yanming Zhou
 */
public class DefaultConstraintModeTest extends BaseUnitTestCase {

	private static final String TABLE_NAME = "TestEntity";

	@Test
	@TestForIssue(jiraKey = "HHH-14253")
	public void testForeignKeyShouldNotBeCreated() {
		testForeignKeyCreation(false);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14253")
	public void testForeignKeyShouldBeCreated() {
		testForeignKeyCreation(true);
	}

	private void testForeignKeyCreation(boolean created) {
		try (StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting(Environment.HBM2DDL_DEFAULT_CONSTRAINT_MODE, created ? "CONSTRAINT" : "NO_CONSTRAINT").build()) {
			Metadata metadata = new MetadataSources( ssr ).addAnnotatedClass( TestEntity.class ).buildMetadata();
			assertThat( findTable( metadata, TABLE_NAME ).getForeignKeys().isEmpty(), is( !created ) );
		}
	}

	private static Table findTable(Metadata metadata, String tableName) {
		return StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(tableName))
				.findFirst().orElse(null);
	}

	@Entity
	@javax.persistence.Table(name = TABLE_NAME)
	public static class TestEntity {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn
		private TestEntity mate;

	}
}
