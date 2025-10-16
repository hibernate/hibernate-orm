/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@EnversTest
@Jpa(annotatedClasses = {BigDecimalTypeTest.BigDecimalEntity.class})
public class BigDecimalTypeTest {
	private Integer entityId;
	private Double bigDecimalValue = 2.2d;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Triggers RevisionType.ADD
		scope.inTransaction( em -> {
			final BigDecimalEntity entity = new BigDecimalEntity( BigDecimal.valueOf( bigDecimalValue ), "Test" );
			em.persist( entity );
			this.entityId = entity.getId();
		} );

		// Should *not* trigger a revision
		scope.inTransaction( em -> {
			final BigDecimalEntity entity = em.find( BigDecimalEntity.class, entityId );
			entity.setData( "Updated" );
			entity.setBigDecimal( BigDecimal.valueOf( bigDecimalValue ) );
			em.merge( entity );
		} );

		// Triggers RevisionType.MOD
		scope.inTransaction( em -> {
			final BigDecimalEntity entity = em.find( BigDecimalEntity.class, entityId );
			entity.setData( "Updated2" );
			entity.setBigDecimal( BigDecimal.valueOf( bigDecimalValue + 1d ) );
			em.merge( entity );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					getAuditReader( em ).getRevisions( BigDecimalEntity.class, entityId ) );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = getAuditReader( em );
			final BigDecimalEntity rev1 = auditReader.find( BigDecimalEntity.class, entityId, 1 );
			assertTrue( BigDecimal.valueOf( bigDecimalValue ).compareTo( rev1.getBigDecimal() ) == 0 );
			assertNull( rev1.getData() );

			final BigDecimalEntity rev2 = auditReader.find( BigDecimalEntity.class, entityId, 2 );
			assertTrue( BigDecimal.valueOf( bigDecimalValue + 1d ).compareTo( rev2.getBigDecimal() ) == 0 );
			assertNull( rev2.getData() );
		} );
	}

	// helper to access the AuditReader (keeps code concise)
	private org.hibernate.envers.AuditReader getAuditReader(EntityManager em) {
		return org.hibernate.envers.AuditReaderFactory.get( em );
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
