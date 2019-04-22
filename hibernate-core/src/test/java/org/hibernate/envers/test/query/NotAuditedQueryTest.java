/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

/**
 * @author Chris Cranford
 */
public class NotAuditedQueryTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NonAuditedEntity.class };
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedMultipleResults() {
		getAuditReader().createQuery()
				.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
				.getResultList();
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedSingleResult() {
		getAuditReader().createQuery()
				.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
				.setMaxResults( 1 )
				.getSingleResult();
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedMultipleResults() {
		getAuditReader().createQuery()
				.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
				.getResultList();
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedSingleResult() {
		getAuditReader().createQuery()
				.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
				.setMaxResults( 1 )
				.getSingleResult();
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedMultipleResults() {
		getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
				.getResultList();
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedSingleResult() {
		getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
				.setMaxResults( 1 )
				.getSingleResult();
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
