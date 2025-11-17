/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.ManyToManyNotAuditedNullEntity;
import org.hibernate.orm.test.envers.entities.manytoone.unidirectional.ExtManyToOneNotAuditedNullEntity;
import org.hibernate.orm.test.envers.entities.manytoone.unidirectional.ManyToOneNotAuditedNullEntity;
import org.hibernate.orm.test.envers.entities.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.orm.test.envers.entities.onetomany.OneToManyNotAuditedNullEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Eugene Goroschenya
 */
@Jpa(annotatedClasses = {
		TargetNotAuditedEntity.class, ManyToOneNotAuditedNullEntity.class, UnversionedStrTestEntity.class,
		ManyToManyNotAuditedNullEntity.class, OneToManyNotAuditedNullEntity.class,
		ExtManyToOneNotAuditedNullEntity.class
})
@EnversTest
public class ProxyIdentifier {
	private TargetNotAuditedEntity tnae1 = null;
	private ManyToOneNotAuditedNullEntity mtonane1 = null;
	private ExtManyToOneNotAuditedNullEntity emtonane1 = null;
	private ManyToManyNotAuditedNullEntity mtmnane1 = null;
	private OneToManyNotAuditedNullEntity otmnane1 = null;
	private UnversionedStrTestEntity uste1 = null;
	private UnversionedStrTestEntity uste2 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		uste1 = new UnversionedStrTestEntity( "str1" );
		uste2 = new UnversionedStrTestEntity( "str2" );

		// No revision
		scope.inTransaction( em -> {
			em.persist( uste1 );
			em.persist( uste2 );
		} );

		// Revision 1
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste1Ref = em.find( UnversionedStrTestEntity.class, uste1.getId() );
			tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1Ref );
			em.persist( tnae1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste2Ref = em.find( UnversionedStrTestEntity.class, uste2.getId() );
			mtonane1 = new ManyToOneNotAuditedNullEntity( 2, "mtonane1", uste2Ref );
			mtmnane1 = new ManyToManyNotAuditedNullEntity( 3, "mtmnane1" );
			mtmnane1.getReferences().add( uste2Ref );
			otmnane1 = new OneToManyNotAuditedNullEntity( 4, "otmnane1" );
			otmnane1.getReferences().add( uste2Ref );
			emtonane1 = new ExtManyToOneNotAuditedNullEntity( 5, "emtonane1", uste2Ref, "extension" );
			em.persist( mtonane1 );
			em.persist( mtmnane1 );
			em.persist( otmnane1 );
			em.persist( emtonane1 );
		} );

		// Revision 3
		// Remove not audited target entity, so we can verify null reference
		// when @NotFound(action = NotFoundAction.IGNORE) applied.
		scope.inTransaction( em -> {
			ManyToOneNotAuditedNullEntity tmp1 = em.find( ManyToOneNotAuditedNullEntity.class, mtonane1.getId() );
			tmp1.setReference( null );
			tmp1 = em.merge( tmp1 );
			ManyToManyNotAuditedNullEntity tmp2 = em.find( ManyToManyNotAuditedNullEntity.class, mtmnane1.getId() );
			tmp2.setReferences( null );
			tmp2 = em.merge( tmp2 );
			OneToManyNotAuditedNullEntity tmp3 = em.find( OneToManyNotAuditedNullEntity.class, otmnane1.getId() );
			tmp3.setReferences( null );
			tmp3 = em.merge( tmp3 );
			ExtManyToOneNotAuditedNullEntity tmp4 = em.find( ExtManyToOneNotAuditedNullEntity.class, emtonane1.getId() );
			tmp4.setReference( null );
			tmp4 = em.merge( tmp4 );
			em.remove( em.getReference( UnversionedStrTestEntity.class, uste2.getId() ) );
		} );
	}

	@Test
	public void testProxyIdentifier(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			TargetNotAuditedEntity rev1 = AuditReaderFactory.get( em ).find( TargetNotAuditedEntity.class, tnae1.getId(), 1 );

			assertTrue( rev1.getReference() instanceof HibernateProxy );

			HibernateProxy proxyCreateByEnvers = (HibernateProxy) rev1.getReference();
			LazyInitializer lazyInitializer = proxyCreateByEnvers.getHibernateLazyInitializer();

			assertTrue( lazyInitializer.isUninitialized() );
			assertNotNull( lazyInitializer.getInternalIdentifier() );
			assertEquals( tnae1.getId(), lazyInitializer.getInternalIdentifier() );
			assertTrue( lazyInitializer.isUninitialized() );

			assertEquals( uste1.getId(), rev1.getReference().getId() );
			assertEquals( uste1.getStr(), rev1.getReference().getStr() );
			assertFalse( lazyInitializer.isUninitialized() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-8174" )
	public void testNullReferenceWithNotFoundActionIgnore(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ManyToOneNotAuditedNullEntity mtoRev2 = AuditReaderFactory.get( em ).find( ManyToOneNotAuditedNullEntity.class, mtonane1.getId(), 2 );
			assertEquals( mtonane1, mtoRev2 );
			assertNull( mtoRev2.getReference() );

			ManyToManyNotAuditedNullEntity mtmRev2 = AuditReaderFactory.get( em ).find( ManyToManyNotAuditedNullEntity.class, mtmnane1.getId(), 2 );
			assertEquals( mtmnane1, mtmRev2 );
			assertTrue( mtmRev2.getReferences().isEmpty() );

			OneToManyNotAuditedNullEntity otmRev2 = AuditReaderFactory.get( em ).find( OneToManyNotAuditedNullEntity.class, otmnane1.getId(), 2 );
			assertEquals( otmnane1, otmRev2 );
			assertTrue( otmRev2.getReferences().isEmpty() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-8912" )
	public void testNullReferenceWithNotFoundActionIgnoreInParent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ExtManyToOneNotAuditedNullEntity emtoRev2 = AuditReaderFactory.get( em ).find( ExtManyToOneNotAuditedNullEntity.class, emtonane1.getId(), 2 );
			assertEquals( emtonane1, emtoRev2 );
			assertNull( emtoRev2.getReference() );
		} );
	}
}
