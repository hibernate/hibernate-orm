package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7003")
public class ColumnScalePrecisionTest extends BaseEnversJPAFunctionalTestCase {
	private TableSpecification auditTable = null;
	private TableSpecification originalTable = null;
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
		auditTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.basic.ScalePrecisionEntity_AUD"
		)
				.getPrimaryTable();
		originalTable = getMetadata().getEntityBinding(
				"org.hibernate.envers.test.integration.basic.ScalePrecisionEntity"
		)
				.getPrimaryTable();
	}

	@Test
	public void testColumnScalePrecision() {
		String columnName = "wholeNumber";
		Column scalePrecisionAuditColumn = auditTable.locateColumn( columnName );
		Column scalePrecisionColumn = originalTable.locateColumn( columnName );

		Assert.assertNotNull( scalePrecisionAuditColumn );
		Assert.assertNotNull( scalePrecisionAuditColumn.getSize() );
		Assert.assertEquals(
				scalePrecisionColumn.getSize().getPrecision(),
				scalePrecisionAuditColumn.getSize().getPrecision()
		);
		Assert.assertEquals( scalePrecisionColumn.getSize().getScale(), scalePrecisionAuditColumn.getSize().getScale() );
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
