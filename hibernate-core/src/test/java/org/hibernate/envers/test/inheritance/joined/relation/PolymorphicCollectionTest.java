/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.joined.relation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.ChildIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.ParentIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.ReferencedEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined Inheritance Support")
public class PolymorphicCollectionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed_id1;
	private Integer c_id;
	private Integer p_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildIngEntity.class, ParentIngEntity.class, ReferencedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		ed_id1 = 1;
		p_id = 10;
		c_id = 100;

		inTransactions(
				// Revision 1
				entityManager -> {
					ReferencedEntity re = new ReferencedEntity( ed_id1 );
					entityManager.persist( re );
				},

				// Revision 2
				entityManager -> {
					ReferencedEntity re = entityManager.find( ReferencedEntity.class, ed_id1 );

					ParentIngEntity pie = new ParentIngEntity( p_id, "x" );
					pie.setReferenced( re );
					entityManager.persist( pie );
					p_id = pie.getId();
				},

				// Revision 3
				entityManager -> {
					ReferencedEntity re = entityManager.find( ReferencedEntity.class, ed_id1 );

					ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
					cie.setReferenced( re );
					entityManager.persist( cie );
					c_id = cie.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ReferencedEntity.class, ed_id1 ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( ParentIngEntity.class, p_id ), contains( 2 ) );
		assertThat( getAuditReader().getRevisions( ChildIngEntity.class, c_id ), contains( 3 ) );
	}

	@DynamicTest
	public void testHistoryOfReferencedCollection() {
		final ParentIngEntity x = new ParentIngEntity( p_id, "x" );
		final ParentIngEntity y = new ChildIngEntity( c_id, "y", 1l );

		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 1 ).getReferencing(), CollectionMatchers.isEmpty() );
		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 2 ).getReferencing(), containsInAnyOrder( x ) );
		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 3 ).getReferencing(), containsInAnyOrder( x, y ) );
	}
}