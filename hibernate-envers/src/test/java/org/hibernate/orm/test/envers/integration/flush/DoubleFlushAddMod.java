/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.util.Arrays;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class})
public class DoubleFlushAddMod {
	private Integer id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Set flush mode to MANUAL
		scope.inTransaction( em -> {
			Session session = em.unwrap( Session.class );
			session.setHibernateFlushMode( FlushMode.MANUAL );

			// Revision 1
			StrTestEntity fe = new StrTestEntity( "x" );
			em.persist( fe );

			em.flush();

			fe.setStr( "y" );

			em.flush();

			id = fe.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			Session session = em.unwrap( Session.class );
			session.setHibernateFlushMode( FlushMode.MANUAL );

			StrTestEntity fe = em.find( StrTestEntity.class, id );

			fe.setStr( "z" );
			em.flush();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ver1 = new StrTestEntity( "y", id );
			StrTestEntity ver2 = new StrTestEntity( "z", id );

			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testRevisionTypes(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			@SuppressWarnings("unchecked") List<Object[]> results =
					AuditReaderFactory.get( em ).createQuery()
							.forRevisionsOfEntity( StrTestEntity.class, false, true )
							.add( AuditEntity.id().eq( id ) )
							.getResultList();

			assertEquals( RevisionType.ADD, results.get( 0 )[2] );
			assertEquals( RevisionType.MOD, results.get( 1 )[2] );
		} );
	}
}
