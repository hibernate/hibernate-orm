/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-4439")
public class AuditClassOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer classAuditedEntityId = null;
	private Integer classNotAuditedEntityId = null;
	private Table classAuditedTable = null;
	private Table classNotAuditedTable = null;

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

		classAuditedTable = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.ClassOverrideAuditedEntity_AUD"
		).getTable();
		classNotAuditedTable = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.ClassOverrideNotAuditedEntity_AUD"
		).getTable();
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( classAuditedTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNotNull( classAuditedTable.getColumn( new Column( "str1" ) ) );
		Assert.assertNotNull( classAuditedTable.getColumn( new Column( "str2" ) ) );
		Assert.assertNotNull( classNotAuditedTable.getColumn( new Column( "str2" ) ) );
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( classNotAuditedTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNull( classNotAuditedTable.getColumn( new Column( "str1" ) ) );
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
