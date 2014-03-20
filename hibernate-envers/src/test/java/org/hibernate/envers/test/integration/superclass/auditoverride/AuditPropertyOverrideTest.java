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
public class AuditPropertyOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer propertyEntityId = null;
	private Integer transitiveEntityId = null;
	private Integer auditedEntityId = null;
	private TableSpecification propertyTable = null;
	private TableSpecification transitiveTable = null;
	private TableSpecification auditedTable = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PropertyOverrideEntity.class, TransitiveOverrideEntity.class, AuditedSpecialEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		PropertyOverrideEntity propertyEntity = new PropertyOverrideEntity( "data 1", 1, "data 2" );
		em.persist( propertyEntity );
		em.getTransaction().commit();
		propertyEntityId = propertyEntity.getId();

		// Revision 2
		em.getTransaction().begin();
		TransitiveOverrideEntity transitiveEntity = new TransitiveOverrideEntity( "data 1", 1, "data 2", 2, "data 3" );
		em.persist( transitiveEntity );
		em.getTransaction().commit();
		transitiveEntityId = transitiveEntity.getId();

		// Revision 3
		em.getTransaction().begin();
		AuditedSpecialEntity auditedEntity = new AuditedSpecialEntity( "data 1", 1, "data 2" );
		em.persist( auditedEntity );
		em.getTransaction().commit();
		auditedEntityId = auditedEntity.getId();

		propertyTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.PropertyOverrideEntity_AUD"
		).getPrimaryTable();
		transitiveTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.TransitiveOverrideEntity_AUD"
		).getPrimaryTable();
		auditedTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.AuditedSpecialEntity_AUD"
		).getPrimaryTable();
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( propertyTable.locateColumn( "str1" ) );
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( propertyTable.locateColumn( "number1" ) );
		Assert.assertNotNull( transitiveTable.locateColumn( "number2" ) );
		Assert.assertNotNull( auditedTable.locateColumn( "str1" ) );
	}

	@Test
	public void testTransitiveAuditedProperty() {
		Assert.assertNotNull( transitiveTable.locateColumn( "number1" ) );
		Assert.assertNotNull( transitiveTable.locateColumn( "str1" ) );
	}

	@Test
	public void testHistoryOfPropertyOverrideEntity() {
		PropertyOverrideEntity ver1 = new PropertyOverrideEntity( null, 1, propertyEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( PropertyOverrideEntity.class, propertyEntityId, 1 ) );
	}

	@Test
	public void testHistoryOfTransitiveOverrideEntity() {
		TransitiveOverrideEntity ver1 = new TransitiveOverrideEntity(
				"data 1",
				1,
				transitiveEntityId,
				"data 2",
				2,
				"data 3"
		);
		Assert.assertEquals( ver1, getAuditReader().find( TransitiveOverrideEntity.class, transitiveEntityId, 2 ) );
	}

	@Test
	public void testHistoryOfAuditedSpecialEntity() {
		AuditedSpecialEntity ver1 = new AuditedSpecialEntity( "data 1", null, auditedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( AuditedSpecialEntity.class, auditedEntityId, 3 ) );
	}
}
