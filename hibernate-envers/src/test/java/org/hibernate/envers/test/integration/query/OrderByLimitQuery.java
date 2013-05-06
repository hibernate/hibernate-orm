/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.query;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class OrderByLimitQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;
	private Integer id5;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IntTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		IntTestEntity ite1 = new IntTestEntity( 12 );
		IntTestEntity ite2 = new IntTestEntity( 5 );
		IntTestEntity ite3 = new IntTestEntity( 8 );
		IntTestEntity ite4 = new IntTestEntity( 1 );

		em.persist( ite1 );
		em.persist( ite2 );
		em.persist( ite3 );
		em.persist( ite4 );

		id1 = ite1.getId();
		id2 = ite2.getId();
		id3 = ite3.getId();
		id4 = ite4.getId();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		IntTestEntity ite5 = new IntTestEntity( 3 );
		em.persist( ite5 );
		id5 = ite5.getId();

		ite1 = em.find( IntTestEntity.class, id1 );
		ite1.setNumber( 0 );

		ite4 = em.find( IntTestEntity.class, id4 );
		ite4.setNumber( 15 );

		em.getTransaction().commit();
	}

	@Test
	public void testEntitiesOrderLimitByQueryRev1() {
		List res_0_to_1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 0 )
				.setMaxResults( 2 )
				.getResultList();

		List res_2_to_3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.getResultList();

		List res_empty = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 4 )
				.setMaxResults( 2 )
				.getResultList();

		assert Arrays.asList( new IntTestEntity( 12, id1 ), new IntTestEntity( 8, id3 ) ).equals( res_0_to_1 );
		assert Arrays.asList( new IntTestEntity( 5, id2 ), new IntTestEntity( 1, id4 ) ).equals( res_2_to_3 );
		assert Arrays.asList().equals( res_empty );
	}

	@Test
	public void testEntitiesOrderLimitByQueryRev2() {
		List res_0_to_1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 0 )
				.setMaxResults( 2 )
				.getResultList();

		List res_2_to_3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.getResultList();

		List res_4 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 4 )
				.setMaxResults( 2 )
				.getResultList();

		assert Arrays.asList( new IntTestEntity( 15, id4 ), new IntTestEntity( 8, id3 ) ).equals( res_0_to_1 );
		assert Arrays.asList( new IntTestEntity( 5, id2 ), new IntTestEntity( 3, id5 ) ).equals( res_2_to_3 );
		assert Arrays.asList( new IntTestEntity( 0, id1 ) ).equals( res_4 );
	}
}