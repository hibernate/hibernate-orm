/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

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