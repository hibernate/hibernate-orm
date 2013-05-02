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
package org.hibernate.envers.test.integration.customtype;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.customtype.ParametrizedCustomTypeEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParametrizedCustom extends BaseEnversJPAFunctionalTestCase {
	private Integer pcte_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ParametrizedCustomTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ParametrizedCustomTypeEntity pcte = new ParametrizedCustomTypeEntity();

		// Revision 1 (persisting 1 entity)
		em.getTransaction().begin();

		pcte.setStr( "U" );

		em.persist( pcte );

		em.getTransaction().commit();

		// Revision 2 (changing the value)
		em.getTransaction().begin();

		pcte = em.find( ParametrizedCustomTypeEntity.class, pcte.getId() );

		pcte.setStr( "V" );

		em.getTransaction().commit();

		//

		pcte_id = pcte.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						ParametrizedCustomTypeEntity.class,
						pcte_id
				)
		);
	}

	@Test
	public void testHistoryOfCcte() {
		ParametrizedCustomTypeEntity rev1 = getAuditReader().find( ParametrizedCustomTypeEntity.class, pcte_id, 1 );
		ParametrizedCustomTypeEntity rev2 = getAuditReader().find( ParametrizedCustomTypeEntity.class, pcte_id, 2 );

		assert "xUy".equals( rev1.getStr() );
		assert "xVy".equals( rev2.getStr() );
	}
}