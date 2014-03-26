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
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.components.relations.OneToManyComponent;
import org.hibernate.envers.test.entities.components.relations.OneToManyComponentTestEntity;
import org.hibernate.envers.test.integration.naming.VersionsJoinTableRangeComponent;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedOneToManyInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer otmcte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				OneToManyComponentTestEntity.class,
				StrTestEntity.class,
				OneToManyComponent.class,
				VersionsJoinTableRangeComponent.class
		};
	}

	@Test
	@Priority(10)
	@FailureExpectedWithNewMetamodel( message = "Collction role is incorrect when contained in an @Embeddable" )
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
		otmcte1.getComp1().getEntities().add( ste1 );

		em.persist( otmcte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		otmcte1 = em.find( OneToManyComponentTestEntity.class, otmcte1.getId() );
		otmcte1.getComp1().getEntities().add( ste2 );

		em.getTransaction().commit();

		otmcte_id1 = otmcte1.getId();
	}

	@Test
	@FailureExpectedWithNewMetamodel( message = "Fails because initData fails." )
	public void testHasChangedId1() throws Exception {
		List list =
				queryForPropertyHasChanged(
						OneToManyComponentTestEntity.class,
						otmcte_id1, "comp1"
				);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				OneToManyComponentTestEntity.class,
				otmcte_id1, "comp1"
		);
		assertEquals( 0, list.size() );
	}
}