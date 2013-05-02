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

package org.hibernate.envers.test.integration.inheritance.joined.relation.unidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Set;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnidirectionalDoubleAbstract extends BaseEnversJPAFunctionalTestCase {
	private Long cce1_id;
	private Integer cse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AbstractContainedEntity.class,
				AbstractSetEntity.class,
				ContainedEntity.class,
				SetEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Rev 1
		em.getTransaction().begin();

		ContainedEntity cce1 = new ContainedEntity();
		em.persist( cce1 );

		SetEntity cse1 = new SetEntity();
		cse1.getEntities().add( cce1 );
		em.persist( cse1 );

		em.getTransaction().commit();

		cce1_id = cce1.getId();
		cse1_id = cse1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( ContainedEntity.class, cce1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( SetEntity.class, cse1_id ) );
	}

	@Test
	public void testHistoryOfReferencedCollection() {
		ContainedEntity cce1 = getEntityManager().find( ContainedEntity.class, cce1_id );

		Set<AbstractContainedEntity> entities = getAuditReader().find( SetEntity.class, cse1_id, 1 ).getEntities();
		assert entities.size() == 1;
		assert entities.iterator().next() instanceof ContainedEntity;
		assert entities.contains( cce1 );
	}
}