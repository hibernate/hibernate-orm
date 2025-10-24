/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.util.Arrays;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey( value = "HHH-8243" )
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class})
@SessionFactory
public class CommitFlush {
	private Integer id = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );

			// Revision 1
			StrTestEntity entity = new StrTestEntity( "x" );
			session.persist( entity );
			session.flush();

			id = entity.getId();
		} );

		// Revision 2
		scope.inTransaction( session -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );

			StrTestEntity entity = session.find( StrTestEntity.class, id );
			entity.setStr( "y" );
			session.merge( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( session ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "y", id );

		scope.inSession( session -> {
			var auditReader = org.hibernate.envers.AuditReaderFactory.get( session );
			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testCurrent(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( new StrTestEntity( "y", id ), session.find( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testRevisionTypes(SessionFactoryScope scope) {
		scope.inSession( session -> {
			var auditReader = org.hibernate.envers.AuditReaderFactory.get( session );
			List<Object[]> results = auditReader.createQuery()
					.forRevisionsOfEntity( StrTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id ) )
					.getResultList();

			assertEquals( RevisionType.ADD, results.get( 0 )[2] );
			assertEquals( RevisionType.MOD, results.get( 1 )[2] );
		} );
	}
}
