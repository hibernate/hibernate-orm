/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
public class NotAuditedQueryTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NonAuditedEntity.class };
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedMultipleResults() {
		try {
			getAuditReader().createQuery()
					.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
					.getResultList();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testRevisionsOfEntityNotAuditedSingleResult() {
		try {
			getAuditReader().createQuery()
					.forRevisionsOfEntity( NonAuditedEntity.class, false, false )
					.setMaxResults( 1 )
					.getSingleResult();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedMultipleResults() {
		try {
			getAuditReader().createQuery()
					.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
					.getResultList();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesAtRevisionNotAuditedSingleResult() {
		try {
			getAuditReader().createQuery()
					.forEntitiesAtRevision( NonAuditedEntity.class, 1 )
					.setMaxResults( 1 )
					.getSingleResult();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedMultipleResults() {
		try {
			getAuditReader().createQuery()
					.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
					.getResultList();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
	}

	@Test
	@JiraKey(value = "HHH-11558")
	public void testForEntitiesModifiedAtRevisionNotAuditedSingleResult() {
		try {
			getAuditReader().createQuery()
					.forEntitiesModifiedAtRevision( NonAuditedEntity.class, 1 )
					.setMaxResults( 1 )
					.getSingleResult();
			fail( "Expected a NotAuditedException" );
		}
		catch ( Exception e ) {
			assertTyping( NotAuditedException.class, e );
		}
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
