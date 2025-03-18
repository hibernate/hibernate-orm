/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7003")
public class ColumnScalePrecisionTest extends BaseEnversJPAFunctionalTestCase {
	private Table auditTable = null;
	private Table originalTable = null;
	private Long id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ScalePrecisionEntity.class};
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
		auditTable = metadata().getEntityBinding( "org.hibernate.orm.test.envers.integration.basic.ScalePrecisionEntity_AUD" )
				.getTable();
		originalTable = metadata().getEntityBinding( "org.hibernate.orm.test.envers.integration.basic.ScalePrecisionEntity" )
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
