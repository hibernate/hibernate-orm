package org.hibernate.test.cfg;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;
import org.hibernate.testing.junit4.TestSessionFactoryHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * This test illustrates the problem when two related (in terms of joins)
 * classes have the same table name in different schemas.
 * 
 * @author Didier Villevalois
 */
@TestForIssue(jiraKey = "HHH-7134")
public class WrongCircularityDetectionTest extends BaseCoreFunctionalTestMethod {

	@Test
	public void testNoCircularityDetection() {
		getTestConfiguration().addAnnotatedClass( Entity1.class ).addAnnotatedClass( Entity2.class );
		getSessionFactoryHelper().setCallback(
				new TestSessionFactoryHelper.CallbackImpl() {
					@Override
					public void afterConfigurationBuilt(
							final Configuration cfg) {
						org.hibernate.mapping.Table entity1Table = cfg.getClassMapping(
								Entity1.class.getName()
						).getTable();
						org.hibernate.mapping.Table entity2Table = cfg.getClassMapping(
								Entity2.class.getName()
						).getTable();

						assertEquals( entity1Table.getName(), entity2Table.getName() ) ;
						assertFalse( entity1Table.getSchema().equals( entity2Table.getSchema() ) );
					}

					@Override
					public void afterMetadataBuilt(final MetadataImplementor metadataImplementor) {
						EntityBinding entity1Binding = metadataImplementor.getEntityBinding( Entity1.class.getName() );
						EntityBinding entity2Binding = metadataImplementor.getEntityBinding( Entity2.class.getName() );

						TableSpecification table1 = entity1Binding.getPrimaryTable();
						TableSpecification table2 = entity2Binding.getPrimaryTable();
						assertEquals( table1.getLogicalName().getText(), table2.getLogicalName().getText() );
						assertFalse( table1.getSchema().equals( table2.getSchema() ) );
					}
				}
		).getSessionFactory();
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
