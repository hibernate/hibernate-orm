package org.hibernate.test.cfg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * This test illustrates the problem when two related (in terms of joins)
 * classes have the same table name in different schemas.
 * 
 * @author Didier Villevalois
 */
@TestForIssue(jiraKey = "HHH-7134")
public class WrongCircularityDetectionTest extends BaseUnitTestCase {

	@Test
	public void testNoCircularityDetection() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass(Entity1.class);
		cfg.addAnnotatedClass(Entity2.class);

		cfg.buildMappings();

		org.hibernate.mapping.Table entity1Table = cfg.getClassMapping(
				Entity1.class.getName()).getTable();
		org.hibernate.mapping.Table entity2Table = cfg.getClassMapping(
				Entity2.class.getName()).getTable();

		assertTrue(entity1Table.getName().equals(entity2Table.getName()));
		assertFalse(entity1Table.getSchema().equals(entity2Table.getSchema()));
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(schema = "schema1", name = "entity")
	public static class Entity1 {
		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Entity
	@Table(schema = "schema2", name = "entity")
	public static class Entity2 extends Entity1 {
		private String value;

		@Basic
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
