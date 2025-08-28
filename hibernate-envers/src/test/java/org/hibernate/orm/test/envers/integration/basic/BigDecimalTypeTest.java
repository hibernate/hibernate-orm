/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.math.BigDecimal;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11988")
public class BigDecimalTypeTest extends BaseEnversJPAFunctionalTestCase {

	private Integer entityId;
	private Double bigDecimalValue = 2.2d;

	@Test
	@Priority(10)
	public void initData() {
		// Triggers RevisionType.ADD
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			final BigDecimalEntity entity = new BigDecimalEntity( BigDecimal.valueOf( bigDecimalValue ), "Test" );
			System.out.println( entity.getBigDecimal().scale() );
			entityManager.persist( entity );
			entityManager.getTransaction().commit();
			this.entityId = entity.getId();
		}
		catch ( Throwable t ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			entityManager.close();
		}

		// Should *not* trigger a revision
		entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			final BigDecimalEntity entity = entityManager.find( BigDecimalEntity.class, entityId );
			entity.setData( "Updated" );
			entity.setBigDecimal( BigDecimal.valueOf( bigDecimalValue ) );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();
		}
		catch ( Throwable t ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			entityManager.close();
		}

		// Triggers RevisionType.MOD
		entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			final BigDecimalEntity entity = entityManager.find( BigDecimalEntity.class, entityId );
			entity.setData( "Updated2" );
			entity.setBigDecimal( BigDecimal.valueOf( bigDecimalValue + 1d ) );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();
		}
		catch ( Throwable t ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( BigDecimalEntity.class, entityId ) );
	}

	@Test
	public void testRevisionHistory() {
		final BigDecimalEntity rev1 = getAuditReader().find( BigDecimalEntity.class, entityId, 1 );
		assertTrue( BigDecimal.valueOf( bigDecimalValue ).compareTo( rev1.getBigDecimal() ) == 0 );
		assertNull( rev1.getData() );

		final BigDecimalEntity rev2 = getAuditReader().find( BigDecimalEntity.class, entityId, 2 );
		assertTrue( BigDecimal.valueOf( bigDecimalValue + 1d ).compareTo( rev2.getBigDecimal() ) == 0 );
		assertNull( rev2.getData() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BigDecimalEntity.class };
	}

	@Entity(name = "BigDecimalEntity")
	@Audited
	public static class BigDecimalEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Column(precision = 23, scale = 6)
		private BigDecimal bigDecimal;
		@NotAudited
		private String data;

		BigDecimalEntity() {

		}

		BigDecimalEntity(BigDecimal bigDecimal, String data) {
			this.bigDecimal = bigDecimal;
			this.data = data;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public BigDecimal getBigDecimal() {
			return bigDecimal;
		}

		public void setBigDecimal(BigDecimal bigDecimal) {
			this.bigDecimal = bigDecimal;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
