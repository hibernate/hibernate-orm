/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.proxy;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.ManyToManyNotAuditedNullEntity;
import org.hibernate.envers.test.entities.manytoone.unidirectional.ExtManyToOneNotAuditedNullEntity;
import org.hibernate.envers.test.entities.manytoone.unidirectional.ManyToOneNotAuditedNullEntity;
import org.hibernate.envers.test.entities.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.envers.test.entities.onetomany.OneToManyNotAuditedNullEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Eugene Goroschenya
 */
public class ProxyIdentifier extends BaseEnversJPAFunctionalTestCase {
	private TargetNotAuditedEntity tnae1 = null;
	private ManyToOneNotAuditedNullEntity mtonane1 = null;
	private ExtManyToOneNotAuditedNullEntity emtonane1 = null;
	private ManyToManyNotAuditedNullEntity mtmnane1 = null;
	private OneToManyNotAuditedNullEntity otmnane1 = null;
	private UnversionedStrTestEntity uste1 = null;
	private UnversionedStrTestEntity uste2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TargetNotAuditedEntity.class, ManyToOneNotAuditedNullEntity.class, UnversionedStrTestEntity.class,
				ManyToManyNotAuditedNullEntity.class, OneToManyNotAuditedNullEntity.class,
				ExtManyToOneNotAuditedNullEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		uste1 = new UnversionedStrTestEntity( "str1" );
		uste2 = new UnversionedStrTestEntity( "str2" );

		// No revision
		em.getTransaction().begin();
		em.persist( uste1 );
		em.persist( uste2 );
		em.getTransaction().commit();

		// Revision 1
		em.getTransaction().begin();
		uste1 = em.find( UnversionedStrTestEntity.class, uste1.getId() );
		tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1 );
		em.persist( tnae1 );
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		uste2 = em.find( UnversionedStrTestEntity.class, uste2.getId() );
		mtonane1 = new ManyToOneNotAuditedNullEntity( 2, "mtonane1", uste2 );
		mtmnane1 = new ManyToManyNotAuditedNullEntity( 3, "mtmnane1" );
		mtmnane1.getReferences().add( uste2 );
		otmnane1 = new OneToManyNotAuditedNullEntity( 4, "otmnane1" );
		otmnane1.getReferences().add( uste2 );
		emtonane1 = new ExtManyToOneNotAuditedNullEntity( 5, "emtonane1", uste2, "extension" );
		em.persist( mtonane1 );
		em.persist( mtmnane1 );
		em.persist( otmnane1 );
		em.persist( emtonane1 );
		em.getTransaction().commit();

		em.clear();

		// Revision 3
		// Remove not audited target entity, so we can verify null reference
		// when @NotFound(action = NotFoundAction.IGNORE) applied.
		em.getTransaction().begin();
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
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testProxyIdentifier() {
		TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1.getId(), 1 );

		Assert.assertTrue( rev1.getReference() instanceof HibernateProxy );

		HibernateProxy proxyCreateByEnvers = (HibernateProxy) rev1.getReference();
		LazyInitializer lazyInitializer = proxyCreateByEnvers.getHibernateLazyInitializer();

		Assert.assertTrue( lazyInitializer.isUninitialized() );
		Assert.assertNotNull( lazyInitializer.getIdentifier() );
		Assert.assertEquals( tnae1.getId(), lazyInitializer.getIdentifier() );
		Assert.assertTrue( lazyInitializer.isUninitialized() );

		Assert.assertEquals( uste1.getId(), rev1.getReference().getId() );
		Assert.assertEquals( uste1.getStr(), rev1.getReference().getStr() );
		Assert.assertFalse( lazyInitializer.isUninitialized() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8174" )
	public void testNullReferenceWithNotFoundActionIgnore() {
		ManyToOneNotAuditedNullEntity mtoRev2 = getAuditReader().find( ManyToOneNotAuditedNullEntity.class, mtonane1.getId(), 2 );
		Assert.assertEquals( mtonane1, mtoRev2 );
		Assert.assertNull( mtoRev2.getReference() );

		ManyToManyNotAuditedNullEntity mtmRev2 = getAuditReader().find( ManyToManyNotAuditedNullEntity.class, mtmnane1.getId(), 2 );
		Assert.assertEquals( mtmnane1, mtmRev2 );
		Assert.assertTrue( mtmRev2.getReferences().isEmpty() );

		OneToManyNotAuditedNullEntity otmRev2 = getAuditReader().find( OneToManyNotAuditedNullEntity.class, otmnane1.getId(), 2 );
		Assert.assertEquals( otmnane1, otmRev2 );
		Assert.assertTrue( otmRev2.getReferences().isEmpty() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8912" )
	public void testNullReferenceWithNotFoundActionIgnoreInParent() {
		ExtManyToOneNotAuditedNullEntity emtoRev2 = getAuditReader().find( ExtManyToOneNotAuditedNullEntity.class, emtonane1.getId(), 2 );
		Assert.assertEquals( emtonane1, emtoRev2 );
		Assert.assertNull( emtoRev2.getReference() );
	}
}
