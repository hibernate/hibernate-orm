/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.unidirectional;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Tomasz Bech
 */
@EnversTest
@Jpa(annotatedClasses = {TargetNotAuditedEntity.class, UnversionedStrTestEntity.class})
public class RelationNotAuditedTarget {
	private Integer tnae1_id;
	private Integer tnae2_id;

	private Integer uste1_id;
	private Integer uste2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// No revision
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste1 = new UnversionedStrTestEntity( "str1" );
			UnversionedStrTestEntity uste2 = new UnversionedStrTestEntity( "str2" );

			em.persist( uste1 );
			em.persist( uste2 );

			uste1_id = uste1.getId();
			uste2_id = uste2.getId();
		} );

		// Revision 1
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			TargetNotAuditedEntity tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1 );
			TargetNotAuditedEntity tnae2 = new TargetNotAuditedEntity( 2, "tnae2", uste2 );
			em.persist( tnae1 );
			em.persist( tnae2 );

			tnae1_id = tnae1.getId();
			tnae2_id = tnae2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			TargetNotAuditedEntity tnae1 = em.find( TargetNotAuditedEntity.class, tnae1_id );
			TargetNotAuditedEntity tnae2 = em.find( TargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			tnae1.setReference( uste2 );
			tnae2.setReference( uste1 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			TargetNotAuditedEntity tnae1 = em.find( TargetNotAuditedEntity.class, tnae1_id );
			TargetNotAuditedEntity tnae2 = em.find( TargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			//field not changed!!!
			tnae1.setReference( uste2 );
			tnae2.setReference( uste2 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			TargetNotAuditedEntity tnae1 = em.find( TargetNotAuditedEntity.class, tnae1_id );
			TargetNotAuditedEntity tnae2 = em.find( TargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );

			tnae1.setReference( uste1 );
			tnae2.setReference( uste1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( TargetNotAuditedEntity.class, tnae1_id );
			assertEquals( Arrays.asList( 1, 2, 4 ), revisions );
			revisions = auditReader.getRevisions( TargetNotAuditedEntity.class, tnae2_id );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), revisions );
		} );
	}

	static Class<?> getClassWithoutInitializingProxy(Object object) {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getPersistentClass();
		}
		else {
			return object.getClass();
		}
	}

	@Test
	public void testHistoryOfTnae1_id(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// load original "tnae1" TargetNotAuditedEntity to force load "str1" UnversionedStrTestEntity as Proxy
			TargetNotAuditedEntity original = em.find( TargetNotAuditedEntity.class, tnae1_id );

			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			TargetNotAuditedEntity rev1 = auditReader.find( TargetNotAuditedEntity.class, tnae1_id, 1 );
			TargetNotAuditedEntity rev2 = auditReader.find( TargetNotAuditedEntity.class, tnae1_id, 2 );
			TargetNotAuditedEntity rev3 = auditReader.find( TargetNotAuditedEntity.class, tnae1_id, 3 );
			TargetNotAuditedEntity rev4 = auditReader.find( TargetNotAuditedEntity.class, tnae1_id, 4 );

			assertEquals( uste1, rev1.getReference() );
			assertEquals( uste2, rev2.getReference() );
			assertEquals( uste2, rev3.getReference() );
			assertEquals( uste1, rev4.getReference() );

			assertTrue( original.getReference() instanceof HibernateProxy );
			assertEquals( UnversionedStrTestEntity.class, Hibernate.getClass( original.getReference() ) );
			assertEquals( UnversionedStrTestEntity.class, getClassWithoutInitializingProxy( rev1.getReference() ) );
			assertEquals( UnversionedStrTestEntity.class, Hibernate.getClass( rev1.getReference() ) );
		} );
	}

	@Test
	public void testHistoryOfTnae2_id(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			TargetNotAuditedEntity rev1 = auditReader.find( TargetNotAuditedEntity.class, tnae2_id, 1 );
			TargetNotAuditedEntity rev2 = auditReader.find( TargetNotAuditedEntity.class, tnae2_id, 2 );
			TargetNotAuditedEntity rev3 = auditReader.find( TargetNotAuditedEntity.class, tnae2_id, 3 );
			TargetNotAuditedEntity rev4 = auditReader.find( TargetNotAuditedEntity.class, tnae2_id, 4 );

			assertEquals( uste2, rev1.getReference() );
			assertEquals( uste1, rev2.getReference() );
			assertEquals( uste2, rev3.getReference() );
			assertEquals( uste1, rev4.getReference() );
		} );
	}
}
