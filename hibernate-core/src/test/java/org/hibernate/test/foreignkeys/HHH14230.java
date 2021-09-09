/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.foreignkeys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.StreamSupport;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14230")
public class HHH14230 {

	private static final String TABLE_NAME = "test_entity";
	private static final String JOIN_COLUMN_NAME = "parent";

	@Test
	public void test() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( TestEntity.class ).buildMetadata();
			Table table = StreamSupport.stream( metadata.getDatabase().getNamespaces().spliterator(), false )
					.flatMap( namespace -> namespace.getTables().stream() )
					.filter( t -> t.getName().equals( TABLE_NAME ) ).findFirst().orElse( null );
			assertNotNull( table );
			assertEquals( 1, table.getForeignKeys().size() );

			// ClassCastException before HHH-14230
			assertTrue( table.getForeignKeys().keySet().iterator().next().toString().contains( JOIN_COLUMN_NAME ) );
		}
	}

	@Entity
	@javax.persistence.Table(name = TABLE_NAME)
	public static class TestEntity {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(name = JOIN_COLUMN_NAME)
		private TestEntity parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TestEntity getParent() {
			return parent;
		}

		public void setParent(TestEntity parent) {
			this.parent = parent;
		}

	}

}
