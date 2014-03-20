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
package org.hibernate.envers.test.integration.components;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.Component2;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Components extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentTestEntity.class, Component1.class, Component2.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a", "b" ), new Component2( "x", "y" ) );
		ComponentTestEntity cte2 = new ComponentTestEntity(
				new Component1( "a2", "b2" ), new Component2(
				"x2",
				"y2"
		)
		);
		ComponentTestEntity cte3 = new ComponentTestEntity(
				new Component1( "a3", "b3" ), new Component2(
				"x3",
				"y3"
		)
		);
		ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

		em.persist( cte1 );
		em.persist( cte2 );
		em.persist( cte3 );
		em.persist( cte4 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );
		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );
		cte3 = em.find( ComponentTestEntity.class, cte3.getId() );
		cte4 = em.find( ComponentTestEntity.class, cte4.getId() );

		cte1.setComp1( new Component1( "a'", "b'" ) );
		cte2.getComp1().setStr1( "a2'" );
		cte3.getComp2().setStr6( "y3'" );
		cte4.setComp1( new Component1() );
		cte4.getComp1().setStr1( "n" );
		cte4.setComp2( new Component2() );
		cte4.getComp2().setStr5( "m" );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );
		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );
		cte3 = em.find( ComponentTestEntity.class, cte3.getId() );
		cte4 = em.find( ComponentTestEntity.class, cte4.getId() );

		cte1.setComp2( new Component2( "x'", "y'" ) );
		cte3.getComp1().setStr2( "b3'" );
		cte4.setComp1( null );
		cte4.setComp2( null );

		em.getTransaction().commit();

		// Revision 4
		em = getEntityManager();
		em.getTransaction().begin();

		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );

		em.remove( cte2 );

		em.getTransaction().commit();

		id1 = cte1.getId();
		id2 = cte2.getId();
		id3 = cte3.getId();
		id4 = cte4.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ComponentTestEntity.class, id1 ) );

		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( ComponentTestEntity.class, id2 ) );

		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( ComponentTestEntity.class, id3 ) );

		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ComponentTestEntity.class, id4 ) );
	}

	@Test
	public void testHistoryOfId1() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a", "b" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "a'", "b'" ), null );

		assert getAuditReader().find( ComponentTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id1, 2 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id1, 3 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id1, 4 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId2() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id2, new Component1( "a2", "b2" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id2, new Component1( "a2'", "b2" ), null );

		assert getAuditReader().find( ComponentTestEntity.class, id2, 1 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id2, 2 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id2, 3 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id2, 4 ) == null;
	}

	@Test
	public void testHistoryOfId3() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id3, new Component1( "a3", "b3" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id3, new Component1( "a3", "b3'" ), null );

		assert getAuditReader().find( ComponentTestEntity.class, id3, 1 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id3, 2 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id3, 3 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id3, 4 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId4() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id4, null, null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id4, new Component1( "n", null ), null );
		ComponentTestEntity ver3 = new ComponentTestEntity( id4, null, null );

		assert getAuditReader().find( ComponentTestEntity.class, id4, 1 ).equals( ver1 );
		assert getAuditReader().find( ComponentTestEntity.class, id4, 2 ).equals( ver2 );
		assert getAuditReader().find( ComponentTestEntity.class, id4, 3 ).equals( ver3 );
		assert getAuditReader().find( ComponentTestEntity.class, id4, 4 ).equals( ver3 );
	}
}
