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
public class MixedOverrideTest extends BaseEnversJPAFunctionalTestCase {
	private Integer mixedEntityId = null;
	private Table mixedTable = null;

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

		mixedTable = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.superclass.auditoverride.MixedOverrideEntity_AUD"
		).getTable();
	}

	@Test
	public void testAuditedProperty() {
		Assert.assertNotNull( mixedTable.getColumn( new Column( "number1" ) ) );
		Assert.assertNotNull( mixedTable.getColumn( new Column( "str2" ) ) );
	}

	@Test
	public void testNotAuditedProperty() {
		Assert.assertNull( mixedTable.getColumn( new Column( "str1" ) ) );
	}

	@Test
	public void testHistoryOfMixedEntity() {
		MixedOverrideEntity ver1 = new MixedOverrideEntity( null, 1, mixedEntityId, "data 2" );
		Assert.assertEquals( ver1, getAuditReader().find( MixedOverrideEntity.class, mixedEntityId, 1 ) );
	}
}