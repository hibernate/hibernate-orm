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
package org.hibernate.envers.test.integration.collection.mapkey;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = " Plural attribute index that is an attribute of the referenced entity is not supported yet." )
public class IdMapKey extends BaseEnversJPAFunctionalTestCase {
	private Integer imke_id;

	private Integer ste1_id;
	private Integer ste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IdMapKeyEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		IdMapKeyEntity imke = new IdMapKeyEntity();

		// Revision 1 (intialy 1 mapping)
		em.getTransaction().begin();

		StrTestEntity ste1 = new StrTestEntity( "x" );
		StrTestEntity ste2 = new StrTestEntity( "y" );

		em.persist( ste1 );
		em.persist( ste2 );

		imke.getIdmap().put( ste1.getId(), ste1 );

		em.persist( imke );

		em.getTransaction().commit();

		// Revision 2 (sse1: adding 1 mapping)
		em.getTransaction().begin();

		ste2 = em.find( StrTestEntity.class, ste2.getId() );
		imke = em.find( IdMapKeyEntity.class, imke.getId() );

		imke.getIdmap().put( ste2.getId(), ste2 );

		em.getTransaction().commit();

		//

		imke_id = imke.getId();

		ste1_id = ste1.getId();
		ste2_id = ste2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( IdMapKeyEntity.class, imke_id ) );
	}

	@Test
	public void testHistoryOfImke() {
		StrTestEntity ste1 = getEntityManager().find( StrTestEntity.class, ste1_id );
		StrTestEntity ste2 = getEntityManager().find( StrTestEntity.class, ste2_id );

		IdMapKeyEntity rev1 = getAuditReader().find( IdMapKeyEntity.class, imke_id, 1 );
		IdMapKeyEntity rev2 = getAuditReader().find( IdMapKeyEntity.class, imke_id, 2 );

		assert rev1.getIdmap().equals( TestTools.makeMap( ste1.getId(), ste1 ) );
		assert rev2.getIdmap().equals( TestTools.makeMap( ste1.getId(), ste1, ste2.getId(), ste2 ) );
	}
}