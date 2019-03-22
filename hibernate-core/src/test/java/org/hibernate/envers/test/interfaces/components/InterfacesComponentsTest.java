/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.interfaces.components;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.interfaces.components.Component1;
import org.hibernate.envers.test.support.domains.interfaces.components.ComponentTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InterfacesComponentsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a" ) );
					entityManager.persist( cte1 );

					this.id1 = cte1.getId();
				},

				// Revision 2
				entityManager -> {
					final ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					cte1.setComp1( new Component1( "b" ) );
				},

				// Revision 3
				entityManager -> {
					final ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					cte1.getComp1().setData( "c" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ComponentTestEntity.class, id1 ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a" ) );
		ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "b" ) );
		ComponentTestEntity ver3 = new ComponentTestEntity( id1, new Component1( "c" ) );

		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 3 ), equalTo( ver3 ) );
	}
}