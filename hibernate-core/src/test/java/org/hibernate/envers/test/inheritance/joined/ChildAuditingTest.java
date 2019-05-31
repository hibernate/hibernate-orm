/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.joined;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.ParentEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined Inheritance Support")
public class ChildAuditingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildEntity.class, ParentEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id1 = 1;

		inTransactions(
				// Revision 1
				entityManager -> {
					ChildEntity ce = new ChildEntity( id1, "x", 1l );
					entityManager.persist( ce );
				},

				// Revision 2
				entityManager -> {
					ChildEntity ce = entityManager.find( ChildEntity.class, id1 );
					ce.setData( "y" );
					ce.setNumVal( 2l );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ChildEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfChildId1() {
		ChildEntity ver1 = new ChildEntity( id1, "x", 1l );
		ChildEntity ver2 = new ChildEntity( id1, "y", 2l );

		assertThat( getAuditReader().find( ChildEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ChildEntity.class, id1, 2 ), equalTo( ver2 ) );

		assertThat( getAuditReader().find( ParentEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ParentEntity.class, id1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testPolymorphicQuery() {
		ChildEntity childVer1 = new ChildEntity( id1, "x", 1l );

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ChildEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);
	}
}