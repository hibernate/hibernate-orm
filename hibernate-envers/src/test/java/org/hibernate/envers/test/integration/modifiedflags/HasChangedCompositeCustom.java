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

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.customtype.Component;
import org.hibernate.envers.test.entities.customtype.CompositeCustomTypeEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedCompositeCustom extends AbstractModifiedFlagsEntityTest {
	private Integer ccte_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CompositeCustomTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		CompositeCustomTypeEntity ccte = new CompositeCustomTypeEntity();

		// Revision 1 (persisting 1 entity)
		em.getTransaction().begin();

		ccte.setComponent( new Component( "a", 1 ) );

		em.persist( ccte );

		em.getTransaction().commit();

		// Revision 2 (changing the component)
		em.getTransaction().begin();

		ccte = em.find( CompositeCustomTypeEntity.class, ccte.getId() );

		ccte.getComponent().setProp1( "b" );

		em.getTransaction().commit();

		// Revision 3 (replacing the component)
		em.getTransaction().begin();

		ccte = em.find( CompositeCustomTypeEntity.class, ccte.getId() );

		ccte.setComponent( new Component( "c", 3 ) );

		em.getTransaction().commit();

		//

		ccte_id = ccte.getId();
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged( CompositeCustomTypeEntity.class, ccte_id, "component" );
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( CompositeCustomTypeEntity.class, ccte_id, "component" );
		assertEquals( 0, list.size() );
	}
}