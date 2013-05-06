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
package org.hibernate.envers.test.integration.modifiedflags;

import javax.persistence.EntityManager;
import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.auditReader.AuditedTestEntity;
import org.hibernate.envers.test.integration.auditReader.NotAuditedTestEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedAPITest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {AuditedTestEntity.class, NotAuditedTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		AuditedTestEntity ent1 = new AuditedTestEntity( 1, "str1" );
		NotAuditedTestEntity ent2 = new NotAuditedTestEntity( 1, "str1" );

		em.persist( ent1 );
		em.persist( ent2 );
		em.getTransaction().commit();

		em.getTransaction().begin();

		ent1 = em.find( AuditedTestEntity.class, 1 );
		ent2 = em.find( NotAuditedTestEntity.class, 1 );
		ent1.setStr1( "str2" );
		ent2.setStr1( "str2" );
		em.getTransaction().commit();
	}

	@Test
	public void testHasChangedHasNotChangedCriteria() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).hasChanged() ).getResultList();
		assertEquals( 2, list.size() );
		assertEquals( "str1", ((AuditedTestEntity) list.get( 0 )).getStr1() );
		assertEquals( "str2", ((AuditedTestEntity) list.get( 1 )).getStr1() );

		list = getAuditReader().createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).hasNotChanged() ).getResultList();
		assertTrue( list.isEmpty() );
	}

}
