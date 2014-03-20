package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-4439")
@FailureExpectedWithNewMetamodel( message = "Audit overrides on MappedSuperclasses not supported yet.")
public class MixedOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer mixedEntityId = null;
	private TableSpecification mixedTable = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MixedOverrideEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		MixedOverrideEntity mixedEntity = new MixedOverrideEntity( "data 1", 1, "data 2" );
		em.persist( mixedEntity );
		em.getTransaction().commit();
		mixedEntityId = mixedEntity.getId();

		mixedTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.MixedOverrideEntity_AUD"
		).getPrimaryTable();
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( mixedTable.locateColumn( "number1" ) );
		Assert.assertNotNull( mixedTable.locateColumn( "str2" ) );
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( mixedTable.locateColumn( "str1" ) );
	}

	@Test
	public void testHistoryOfMixedEntity() {
		MixedOverrideEntity ver1 = new MixedOverrideEntity( null, 1, mixedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( MixedOverrideEntity.class, mixedEntityId, 1 ) );
	}
}