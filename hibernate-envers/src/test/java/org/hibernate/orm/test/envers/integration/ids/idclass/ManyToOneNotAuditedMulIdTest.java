/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.idclass;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-14280")
@EnversTest
@Jpa(annotatedClasses = {UnversionedStrTestEntity.class, ManyToOneNotAuditedMulIdTest.ManyToOneNotAuditedMulIdTestEntity.class})
public class ManyToOneNotAuditedMulIdTest {
	private final static String str1 = "str1", str2 = "str2";

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			UnversionedStrTestEntity id2 = new UnversionedStrTestEntity( str1 );
			ManyToOneNotAuditedMulIdTestEntity entity = new ManyToOneNotAuditedMulIdTestEntity( id2, str2 );

			em.persist( id2 );
			em.persist( entity );
		} );
	}

	@Test
	public void testRevisionQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final List<ManyToOneNotAuditedMulIdTestEntity> entities = auditReader.createQuery()
					.forRevisionsOfEntity( ManyToOneNotAuditedMulIdTestEntity.class, true, true )
					.getResultList();

			assertEquals( 1, entities.size() );
			final ManyToOneNotAuditedMulIdTestEntity entity = entities.get( 0 );
			assertEquals( str1, entity.id2.getStr() );
			assertEquals( str2, entity.str );
		} );
	}

	@Entity(name = "ManyToOneNotAudited")
	@IdClass(MulId.class)
	public static class ManyToOneNotAuditedMulIdTestEntity {
		@Id
		@GeneratedValue
		private Integer id1;

		@Id
		@ManyToOne
		private UnversionedStrTestEntity id2;

		@Audited
		private String str;

		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		public UnversionedStrTestEntity getId2() {
			return id2;
		}

		public void setId2(UnversionedStrTestEntity id2) {
			this.id2 = id2;
		}

		public String getStr() {
			return str;
		}

		public void setStr(String str) {
			this.str = str;
		}

		public ManyToOneNotAuditedMulIdTestEntity() {
		}

		public ManyToOneNotAuditedMulIdTestEntity(UnversionedStrTestEntity id2, String str) {
			this.id2 = id2;
			this.str = str;
		}
	}
}
