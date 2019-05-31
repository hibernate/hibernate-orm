/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.joined.emptychild;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.emptychild.EmptyChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.emptychild.ParentEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeEach;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined Inheritance Support")
public class EmptyChildAuditingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmptyChildEntity.class, ParentEntity.class };
	}

	@DynamicBeforeEach
	public void prepareAuditData() {
		this.id1 = 1;

		inTransactions(
				// Revision 1
				entityManager -> {
					EmptyChildEntity pe = new EmptyChildEntity( id1, "x" );
					entityManager.persist( pe );
				},

				// Revision 2
				entityManager -> {
					EmptyChildEntity pe = entityManager.find( EmptyChildEntity.class, id1 );
					pe.setData( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmptyChildEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfChildId1() {
		EmptyChildEntity ver1 = new EmptyChildEntity( id1, "x" );
		EmptyChildEntity ver2 = new EmptyChildEntity( id1, "y" );

		assertThat( getAuditReader().find( EmptyChildEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmptyChildEntity.class, id1, 2 ), equalTo( ver2 ) );

		assertThat( getAuditReader().find( ParentEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ParentEntity.class, id1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testPolymorphicQuery() {
		EmptyChildEntity childVer1 = new EmptyChildEntity( id1, "x" );

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( EmptyChildEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);
	}
}
