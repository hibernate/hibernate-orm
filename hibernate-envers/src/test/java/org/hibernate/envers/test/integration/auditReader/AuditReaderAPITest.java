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
package org.hibernate.envers.test.integration.auditReader;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 */
public class AuditReaderAPITest extends BaseEnversJPAFunctionalTestCase {
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
	public void testIsEntityClassAuditedForAuditedEntity() {
		assert getAuditReader().isEntityClassAudited( AuditedTestEntity.class );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( AuditedTestEntity.class, 1 ) );
	}

	@Test
	public void testIsEntityClassAuditedForNotAuditedEntity() {

		assert !getAuditReader().isEntityClassAudited( NotAuditedTestEntity.class );

		try {
			getAuditReader().getRevisions( NotAuditedTestEntity.class, 1 );
		}
		catch (NotAuditedException nae) {
			// it's ok because the entity is not audited
			assert true;
		}
	}


}
