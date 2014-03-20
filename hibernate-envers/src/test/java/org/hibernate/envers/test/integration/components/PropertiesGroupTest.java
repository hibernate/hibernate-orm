package org.hibernate.envers.test.integration.components;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.UniquePropsEntity;
import org.hibernate.envers.test.entities.components.UniquePropsNotAuditedEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6636")
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class PropertiesGroupTest extends BaseEnversFunctionalTestCase {
	private EntityBinding uniquePropsAudit = null;
	private EntityBinding uniquePropsNotAuditedAudit = null;
	private UniquePropsEntity entityRev1 = null;
	private UniquePropsNotAuditedEntity entityNotAuditedRev2 = null;

	@Override
	protected String[] getMappings() {
		return new String[] {
				"mappings/components/UniquePropsEntity.hbm.xml",
				"mappings/components/UniquePropsNotAuditedEntity.hbm.xml"
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		uniquePropsAudit = metadata().getEntityBinding(
				"org.hibernate.envers.test.entities.components.UniquePropsEntity_AUD"
		);
		uniquePropsNotAuditedAudit = metadata().getEntityBinding(
				"org.hibernate.envers.test.entities.components.UniquePropsNotAuditedEntity_AUD"
		);

		// Revision 1
		Session session = openSession();
		session.getTransaction().begin();
		UniquePropsEntity ent = new UniquePropsEntity();
		ent.setData1( "data1" );
		ent.setData2( "data2" );
		session.persist( ent );
		session.getTransaction().commit();

		entityRev1 = new UniquePropsEntity( ent.getId(), ent.getData1(), ent.getData2() );

		// Revision 2
		session.getTransaction().begin();
		UniquePropsNotAuditedEntity entNotAud = new UniquePropsNotAuditedEntity();
		entNotAud.setData1( "data3" );
		entNotAud.setData2( "data4" );
		session.persist( entNotAud );
		session.getTransaction().commit();

		entityNotAuditedRev2 = new UniquePropsNotAuditedEntity( entNotAud.getId(), entNotAud.getData1(), null );
	}

	@Test
	public void testAuditTableColumns() {
		Assert.assertNotNull( uniquePropsAudit.getPrimaryTable().locateColumn( "DATA1" ) );
		Assert.assertNotNull( uniquePropsAudit.getPrimaryTable().locateColumn( "DATA2" ) );

		Assert.assertNotNull( uniquePropsNotAuditedAudit.getPrimaryTable().locateColumn(  "DATA1" ) );
		Assert.assertNull( uniquePropsNotAuditedAudit.getPrimaryTable().locateColumn( "DATA2" ) );
	}

	@Test
	public void testHistoryOfUniquePropsEntity() {
		Assert.assertEquals( entityRev1, getAuditReader().find( UniquePropsEntity.class, entityRev1.getId(), 1 ) );
	}

	@Test
	public void testHistoryOfUniquePropsNotAuditedEntity() {
		Assert.assertEquals(
				entityNotAuditedRev2, getAuditReader().find(
				UniquePropsNotAuditedEntity.class,
				entityNotAuditedRev2.getId(),
				2
		)
		);
	}
}
