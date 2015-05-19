/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.customtype.Component;
import org.hibernate.envers.test.entities.customtype.CompositeCustomTypeEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeCustom extends BaseEnversJPAFunctionalTestCase {
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
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals(
				getAuditReader().getRevisions(
						CompositeCustomTypeEntity.class,
						ccte_id
				)
		);
	}

	@Test
	public void testHistoryOfCcte() {
		CompositeCustomTypeEntity rev1 = getAuditReader().find( CompositeCustomTypeEntity.class, ccte_id, 1 );
		CompositeCustomTypeEntity rev2 = getAuditReader().find( CompositeCustomTypeEntity.class, ccte_id, 2 );
		CompositeCustomTypeEntity rev3 = getAuditReader().find( CompositeCustomTypeEntity.class, ccte_id, 3 );

		assert rev1.getComponent().equals( new Component( "a", 1 ) );
		assert rev2.getComponent().equals( new Component( "b", 1 ) );
		assert rev3.getComponent().equals( new Component( "c", 3 ) );
	}
}