package org.hibernate.envers.test.integration.inheritance.tableperclass.abstractparent;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5910")
public class AuditedAbstractParentTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {AbstractEntity.class, EffectiveEntity1.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		EffectiveEntity1 entity = new EffectiveEntity1( 1L, "commonField", "specificField1" );
		em.persist( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testAbstractTableExistence() {
		for ( Schema schema : getMetadata().getDatabase().getSchemas() ) {
			for ( TableSpecification table : schema.getTables() ) {
				if ( "AbstractEntity_AUD".equals( table.getLogicalName().getText() ) ) {
					Assert.assertTrue( Table.class.isInstance( table ) );
					Assert.assertFalse( Table.class.cast( table ).isPhysicalTable() );
					return;
				}
			}
		}
		Assert.fail();
	}
}
