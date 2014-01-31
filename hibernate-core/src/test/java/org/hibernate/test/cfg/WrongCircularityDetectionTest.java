package org.hibernate.test.cfg;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.relational.Schema;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
		MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Entity1.class )
				.addAnnotatedClass( Entity2.class );
		MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

		org.hibernate.metamodel.spi.relational.Table entity1Table = null;
		org.hibernate.metamodel.spi.relational.Table entity2Table = null;

		int schemaNumber = 0;
		boolean foundSchema1 = false;
		boolean foundSchema2 = false;

		for ( Schema schema : metadataImplementor.getDatabase().getSchemas() ) {
			schemaNumber = -1;

			if ( schema.getName().getSchema() == null ) {
				continue;
			}

			if ( "schema1".equals( schema.getName().getSchema().getText() ) ) {
				foundSchema1 = true;
				schemaNumber = 1;
			}
			else if ( "schema2".equals( schema.getName().getSchema().getText() ) ) {
				foundSchema2 = true;
				schemaNumber = 2;
			}

			for ( org.hibernate.metamodel.spi.relational.Table table : schema.getTables() ) {
				if ( "entity".equals( table.getPhysicalName().getText() ) ) {
					if ( schemaNumber == 1 ) {
						entity1Table = table;
					}
					else if ( schemaNumber == 2 ) {
						entity2Table = table;
					}
				}
			}
		}

		assertTrue( foundSchema1 );
		assertTrue( foundSchema2 );
		assertNotNull( entity1Table );
		assertNotNull( entity2Table );
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
