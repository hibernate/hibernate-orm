/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.single.relation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.single.relation.ChildIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.relation.ParentIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.relation.ReferencedEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Inheritance")
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
		inTransactions(
				// Revision 1
				entityManager -> {
					ReferencedEntity re = new ReferencedEntity();
					entityManager.persist( re );
					ed_id1 = re.getId();
				},

				// Revision 2
				entityManager -> {
					ReferencedEntity re = entityManager.find( ReferencedEntity.class, ed_id1 );

					ParentIngEntity pie = new ParentIngEntity( "x" );
					pie.setReferenced( re );
					entityManager.persist( pie );
					p_id = pie.getId();
				},

				// Revision 3
				entityManager -> {
					ReferencedEntity re = entityManager.find( ReferencedEntity.class, ed_id1 );

					ChildIngEntity cie = new ChildIngEntity( "y", 1l );
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
		final ParentIngEntity parent = new ParentIngEntity( p_id, "x" );
		final ChildIngEntity child = new ChildIngEntity( c_id, "y", 1l );

		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 1 ).getReferencing(), isEmpty() );
		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 2 ).getReferencing(), containsInAnyOrder( parent ) );
		assertThat( getAuditReader().find( ReferencedEntity.class, ed_id1, 3 ).getReferencing(), containsInAnyOrder( parent, child ) );
	}
}