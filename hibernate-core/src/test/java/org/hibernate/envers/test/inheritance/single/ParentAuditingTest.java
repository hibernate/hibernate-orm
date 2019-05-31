/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.single;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.single.ChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.ParentEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Inheritance")
public class ParentAuditingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildEntity.class, ParentEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					ParentEntity pe = new ParentEntity( "x" );
					entityManager.persist( pe );
					id1 = pe.getId();
				},

				// Revision 2
				entityManager -> {
					ParentEntity pe = entityManager.find( ParentEntity.class, id1 );
					pe.setData( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ParentEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfChildId1() {
		assertThat( getAuditReader().find( ChildEntity.class, id1, 1 ), nullValue() );
		assertThat( getAuditReader().find( ChildEntity.class, id1, 2 ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfParentId1() {
		final ParentEntity ver1 = new ParentEntity( id1, "x" );
		final ParentEntity ver2 = new ParentEntity( id1, "y" );

		assertThat( getAuditReader().find( ParentEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ParentEntity.class, id1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testPolymorphicQuery() {
		final ParentEntity parentVer1 = new ParentEntity( id1, "x" );

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult(),
				equalTo( parentVer1 )
		);
		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ChildEntity.class, 1 ).getResultList(),
				CollectionMatchers.isEmpty()
		);
	}
}