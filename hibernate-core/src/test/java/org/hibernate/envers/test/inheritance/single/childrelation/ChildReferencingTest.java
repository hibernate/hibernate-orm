/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.single.childrelation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.single.childrelation.ChildIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.childrelation.ParentNotIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.childrelation.ReferencedEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Inheritance support")
public class ChildReferencingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer re_id1;
	private Integer re_id2;
	private Integer c_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					ReferencedEntity re1 = new ReferencedEntity();
					entityManager.persist( re1 );
					re_id1 = re1.getId();

					ReferencedEntity re2 = new ReferencedEntity();
					entityManager.persist( re2 );
					re_id2 = re2.getId();
				},

				// Revision 2
				entityManager -> {
					ReferencedEntity re1 = entityManager.find( ReferencedEntity.class, re_id1 );

					ChildIngEntity cie = new ChildIngEntity( "y", 1l );
					cie.setReferenced( re1 );
					entityManager.persist( cie );
					c_id = cie.getId();
				},

				// Revision 3
				entityManager -> {
					ReferencedEntity re2 = entityManager.find( ReferencedEntity.class, re_id2 );
					ChildIngEntity cie = entityManager.find( ChildIngEntity.class, c_id );

					cie.setReferenced( re2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ReferencedEntity.class, re_id1 ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( ReferencedEntity.class, re_id2 ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( ChildIngEntity.class, c_id ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfReferencedCollection1() {
		final ChildIngEntity child = new ChildIngEntity( c_id, "y", 1l );

		assertThat( getAuditReader().find( ReferencedEntity.class, re_id1, 1 ).getReferencing(), isEmpty() );
		assertThat( getAuditReader().find( ReferencedEntity.class, re_id1, 2 ).getReferencing(), contains( child ) );
		assertThat( getAuditReader().find( ReferencedEntity.class, re_id1, 3 ).getReferencing(), isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfReferencedCollection2() {
		final ChildIngEntity entity = new ChildIngEntity( c_id, "y", 1l );

		assertThat( getAuditReader().find( ReferencedEntity.class, re_id2, 1 ).getReferencing(), isEmpty() );
		assertThat( getAuditReader().find( ReferencedEntity.class, re_id2, 2 ).getReferencing(), isEmpty() );
		assertThat( getAuditReader().find( ReferencedEntity.class, re_id2, 3 ).getReferencing(), contains( entity ) );
	}

	@DynamicTest
	public void testChildHistory() {
		final ReferencedEntity ver2 = new ReferencedEntity( re_id1 );
		final ReferencedEntity ver3 = new ReferencedEntity( re_id2 );

		assertThat( getAuditReader().find( ChildIngEntity.class, c_id, 1 ), nullValue() );
		assertThat( getAuditReader().find( ChildIngEntity.class, c_id, 2 ).getReferenced(), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ChildIngEntity.class, c_id, 3 ).getReferenced(), equalTo( ver3 ) );
	}
}