/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Arrays;
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
@TestForIssue(jiraKey = "HHH-7003")
public class ColumnScalePrecisionTest extends BaseEnversJPAFunctionalTestCase {
	private Table auditTable = null;
	private Table originalTable = null;
	private Long id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ScalePrecisionEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		ScalePrecisionEntity entity = new ScalePrecisionEntity( 13.0 );
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();
		auditTable = metadata().getEntityBinding( "org.hibernate.envers.test.integration.basic.ScalePrecisionEntity_AUD" )
				.getTable();
		originalTable = metadata().getEntityBinding( "org.hibernate.envers.test.integration.basic.ScalePrecisionEntity" )
				.getTable();
	}

	@Test
	public void testColumnScalePrecision() {
		Column testColumn = new Column( "wholeNumber" );
		Column scalePrecisionAuditColumn = auditTable.getColumn( testColumn );
		Column scalePrecisionColumn = originalTable.getColumn( testColumn );

		Assert.assertNotNull( scalePrecisionAuditColumn );
		Assert.assertEquals( scalePrecisionColumn.getPrecision(), scalePrecisionAuditColumn.getPrecision() );
		Assert.assertEquals( scalePrecisionColumn.getScale(), scalePrecisionAuditColumn.getScale() );
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( ScalePrecisionEntity.class, id ) );
	}

	@Test
	public void testHistoryOfScalePrecisionEntity() {
		ScalePrecisionEntity ver1 = new ScalePrecisionEntity( 13.0, id );

		Assert.assertEquals( ver1, getAuditReader().find( ScalePrecisionEntity.class, id, 1 ) );
	}
}
