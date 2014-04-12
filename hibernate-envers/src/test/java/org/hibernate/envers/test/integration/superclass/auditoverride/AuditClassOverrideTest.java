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
@FailureExpectedWithNewMetamodel( message = "@MappedSuperclass not supported with new metamodel by envers yet.")
public class AuditClassOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer classAuditedEntityId = null;
	private Integer classNotAuditedEntityId = null;
	private TableSpecification classAuditedTable = null;
	private TableSpecification classNotAuditedTable = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ClassOverrideAuditedEntity.class, ClassOverrideNotAuditedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		ClassOverrideAuditedEntity classOverrideAuditedEntity = new ClassOverrideAuditedEntity( "data 1", 1, "data 2" );
		em.persist( classOverrideAuditedEntity );
		em.getTransaction().commit();
		classAuditedEntityId = classOverrideAuditedEntity.getId();

		// Revision 2
		em.getTransaction().begin();
		ClassOverrideNotAuditedEntity classOverrideNotAuditedEntity = new ClassOverrideNotAuditedEntity(
				"data 1",
				1,
				"data 2"
		);
		em.persist( classOverrideNotAuditedEntity );
		em.getTransaction().commit();
		classNotAuditedEntityId = classOverrideNotAuditedEntity.getId();

		classAuditedTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.ClassOverrideAuditedEntity_AUD"
		).getPrimaryTable();
		classNotAuditedTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.ClassOverrideNotAuditedEntity_AUD"
		).getPrimaryTable();
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( classAuditedTable.locateColumn( "number1" ) );
		Assert.assertNotNull( classAuditedTable.locateColumn( "str1" ) );
		Assert.assertNotNull( classAuditedTable.locateColumn( "str2" ) );
		Assert.assertNotNull( classNotAuditedTable.locateColumn( "str2" ) );
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( classNotAuditedTable.locateColumn( "number1" ) );
		Assert.assertNull( classNotAuditedTable.locateColumn( "str1" ) );
	}

	@Test
	public void testHistoryOfClassOverrideAuditedEntity() {
		ClassOverrideAuditedEntity ver1 = new ClassOverrideAuditedEntity( "data 1", 1, classAuditedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( ClassOverrideAuditedEntity.class, classAuditedEntityId, 1 ) );
	}

	@Test
	public void testHistoryOfClassOverrideNotAuditedEntity() {
		ClassOverrideNotAuditedEntity ver1 = new ClassOverrideNotAuditedEntity(
				null,
				null,
				classNotAuditedEntityId,
				"data 2"
		);
		Assert.assertEquals(
				ver1, getAuditReader().find(
				ClassOverrideNotAuditedEntity.class,
				classNotAuditedEntityId,
				2
		)
		);
	}
}
