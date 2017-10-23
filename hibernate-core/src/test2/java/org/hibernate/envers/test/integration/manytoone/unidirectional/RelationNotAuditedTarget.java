/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.unidirectional;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.manytoone.unidirectional.TargetNotAuditedEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;

import org.junit.Test;

/**
 * @author Tomasz Bech
 */
public class RelationNotAuditedTarget extends BaseEnversJPAFunctionalTestCase {
	private Integer tnae1_id;
	private Integer tnae2_id;

	private Integer uste1_id;
	private Integer uste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TargetNotAuditedEntity.class, UnversionedStrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		UnversionedStrTestEntity uste1 = new UnversionedStrTestEntity( "str1" );
		UnversionedStrTestEntity uste2 = new UnversionedStrTestEntity( "str2" );

		// No revision
		em.getTransaction().begin();

		em.persist( uste1 );
		em.persist( uste2 );

		em.getTransaction().commit();

		// Revision 1
		em.getTransaction().begin();

		uste1 = em.find( UnversionedStrTestEntity.class, uste1.getId() );
		uste2 = em.find( UnversionedStrTestEntity.class, uste2.getId() );

		TargetNotAuditedEntity tnae1 = new TargetNotAuditedEntity( 1, "tnae1", uste1 );
		TargetNotAuditedEntity tnae2 = new TargetNotAuditedEntity( 2, "tnae2", uste2 );
		em.persist( tnae1 );
		em.persist( tnae2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		tnae1 = em.find( TargetNotAuditedEntity.class, tnae1.getId() );
		tnae2 = em.find( TargetNotAuditedEntity.class, tnae2.getId() );

		tnae1.setReference( uste2 );
		tnae2.setReference( uste1 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		tnae1 = em.find( TargetNotAuditedEntity.class, tnae1.getId() );
		tnae2 = em.find( TargetNotAuditedEntity.class, tnae2.getId() );

		//field not changed!!!
		tnae1.setReference( uste2 );
		tnae2.setReference( uste2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		tnae1 = em.find( TargetNotAuditedEntity.class, tnae1.getId() );
		tnae2 = em.find( TargetNotAuditedEntity.class, tnae2.getId() );

		tnae1.setReference( uste1 );
		tnae2.setReference( uste1 );

		em.getTransaction().commit();

		//
		tnae1_id = tnae1.getId();
		tnae2_id = tnae2.getId();
		uste1_id = uste1.getId();
		uste2_id = uste2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		List<Number> revisions = getAuditReader().getRevisions( TargetNotAuditedEntity.class, tnae1_id );
		assert Arrays.asList( 1, 2, 4 ).equals( revisions );
		revisions = getAuditReader().getRevisions( TargetNotAuditedEntity.class, tnae2_id );
		assert Arrays.asList( 1, 2, 3, 4 ).equals( revisions );
	}

	@Test
	public void testHistoryOfTnae1_id() {
		// load original "tnae1" TargetNotAuditedEntity to force load "str1" UnversionedStrTestEntity as Proxy
		TargetNotAuditedEntity original = getEntityManager().find( TargetNotAuditedEntity.class, tnae1_id );

		UnversionedStrTestEntity uste1 = getEntityManager().find( UnversionedStrTestEntity.class, uste1_id );
		UnversionedStrTestEntity uste2 = getEntityManager().find( UnversionedStrTestEntity.class, uste2_id );

		TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 1 );
		TargetNotAuditedEntity rev2 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 2 );
		TargetNotAuditedEntity rev3 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 3 );
		TargetNotAuditedEntity rev4 = getAuditReader().find( TargetNotAuditedEntity.class, tnae1_id, 4 );

		assert rev1.getReference().equals( uste1 );
		assert rev2.getReference().equals( uste2 );
		assert rev3.getReference().equals( uste2 );
		assert rev4.getReference().equals( uste1 );

		assert original.getReference() instanceof HibernateProxy;
		assert UnversionedStrTestEntity.class.equals( Hibernate.getClass( original.getReference() ) );
		assert UnversionedStrTestEntity.class.equals( HibernateProxyHelper.getClassWithoutInitializingProxy( rev1.getReference() ) );
		assert UnversionedStrTestEntity.class.equals( Hibernate.getClass( rev1.getReference() ) );
	}

	@Test
	public void testHistoryOfTnae2_id() {
		UnversionedStrTestEntity uste1 = getEntityManager().find( UnversionedStrTestEntity.class, uste1_id );
		UnversionedStrTestEntity uste2 = getEntityManager().find( UnversionedStrTestEntity.class, uste2_id );

		TargetNotAuditedEntity rev1 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 1 );
		TargetNotAuditedEntity rev2 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 2 );
		TargetNotAuditedEntity rev3 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 3 );
		TargetNotAuditedEntity rev4 = getAuditReader().find( TargetNotAuditedEntity.class, tnae2_id, 4 );

		assert rev1.getReference().equals( uste2 );
		assert rev2.getReference().equals( uste1 );
		assert rev3.getReference().equals( uste2 );
		assert rev4.getReference().equals( uste1 );
	}
}
