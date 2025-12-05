/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {NotAuditedQueryTest.NonAuditedEntity.class})
@EnversTest
public class NotAuditedQueryTest {

	@Test
	@JiraKey(value = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedMultipleResults(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
				.getResultList() ) );
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedSingleResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
				.setMaxResults( 1 )
				.getSingleResult() ) );
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedMultipleResults(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
				.getResultList() ) );
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedSingleResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
				.setMaxResults( 1 )
				.getSingleResult() ) );
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedMultipleResults(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
				.getResultList() ) );
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedSingleResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> assertThrows( NotAuditedException.class, () -> AuditReaderFactory.get( em ).createQuery()
				.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
				.setMaxResults( 1 )
				.getSingleResult() ) );
	}

	@Entity(name = "NonAuditedEntity")
	public static class NonAuditedEntity {
		@Id
		private Integer id;
		private String data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
