/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.biowned;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.biowned.ListBiowning1Entity;
import org.hibernate.envers.test.support.domains.manytomany.biowned.ListBiowning2Entity;
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
@Disabled("NYI - Bidirectional @ManyToMany with no inverse, both sides own the relationship")
public class BasicBiownedTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer o1_1_id;
	private Integer o1_2_id;
	private Integer o2_1_id;
	private Integer o2_2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ListBiowning1Entity.class, ListBiowning2Entity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					ListBiowning1Entity o1_1 = new ListBiowning1Entity( "o1_1" );
					ListBiowning1Entity o1_2 = new ListBiowning1Entity( "o1_2" );
					ListBiowning2Entity o2_1 = new ListBiowning2Entity( "o2_1" );
					ListBiowning2Entity o2_2 = new ListBiowning2Entity( "o2_2" );

					entityManager.persist( o1_1 );
					entityManager.persist( o1_2 );
					entityManager.persist( o2_1 );
					entityManager.persist( o2_2 );

					o1_1_id = o1_1.getId();
					o1_2_id = o1_2.getId();
					o2_1_id = o2_1.getId();
					o2_2_id = o2_2.getId();
				},

				// Revision 2 (1_1 <-> 2_1; 1_2 <-> 2_2)
				entityManager -> {
					entityManager.clear();

					ListBiowning1Entity o1_1 = entityManager.find( ListBiowning1Entity.class, o1_1_id );
					ListBiowning1Entity o1_2 = entityManager.find( ListBiowning1Entity.class, o1_2_id );
					ListBiowning2Entity o2_1 = entityManager.find( ListBiowning2Entity.class, o2_1_id );
					ListBiowning2Entity o2_2 = entityManager.find( ListBiowning2Entity.class, o2_2_id );

					o1_1.getReferences().add( o2_1 );
					o1_2.getReferences().add( o2_2 );
				},

				// Revision 3 (1_1 <-> 2_1, 2_2; 1_2 <-> 2_2)
				entityManager -> {
					entityManager.clear();

					ListBiowning1Entity o1_1 = entityManager.find( ListBiowning1Entity.class, o1_1_id );
					ListBiowning2Entity o2_2 = entityManager.find( ListBiowning2Entity.class, o2_2_id );

					o1_1.getReferences().add( o2_2 );
				},

				// Revision 4 (1_2 <-> 2_1, 2_2)
				entityManager -> {
					entityManager.clear();

					ListBiowning1Entity o1_1 = entityManager.find( ListBiowning1Entity.class, o1_1_id );
					ListBiowning1Entity o1_2 = entityManager.find( ListBiowning1Entity.class, o1_2_id );
					ListBiowning2Entity o2_1 = entityManager.find( ListBiowning2Entity.class, o2_1_id );
					ListBiowning2Entity o2_2 = entityManager.find( ListBiowning2Entity.class, o2_2_id );

					o2_2.getReferences().remove( o1_1 );
					o2_1.getReferences().remove( o1_1 );
					o2_1.getReferences().add( o1_2 );
				},

				// Revision 5 (1_1 <-> 2_2, 1_2 <-> 2_2)
				entityManager -> {
					entityManager.clear();

					ListBiowning1Entity o1_1 = entityManager.find( ListBiowning1Entity.class, o1_1_id );
					ListBiowning1Entity o1_2 = entityManager.find( ListBiowning1Entity.class, o1_2_id );
					ListBiowning2Entity o2_1 = entityManager.find( ListBiowning2Entity.class, o2_1_id );
					ListBiowning2Entity o2_2 = entityManager.find( ListBiowning2Entity.class, o2_2_id );

					o1_2.getReferences().remove( o2_1 );
					o1_1.getReferences().add( o2_2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		// Although it would seem that when modifying references both entities should be marked as modified, because
		// only the owning side is notified (because of the bi-owning mapping), a revision is created only for
		// the entity where the collection was directly modified.
		assertThat( getAuditReader().getRevisions( ListBiowning1Entity.class, o1_1_id ), contains( 1, 2, 3, 5 ) );
		assertThat( getAuditReader().getRevisions( ListBiowning1Entity.class, o1_2_id ), contains( 1, 2, 5 ) );

		assertThat( getAuditReader().getRevisions( ListBiowning2Entity.class, o2_1_id ), contains( 1, 4 ) );
		assertThat( getAuditReader().getRevisions( ListBiowning2Entity.class, o2_2_id ), contains( 1, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfO1_1() {
		ListBiowning2Entity o2_1 = inTransaction( em -> { return em.find( ListBiowning2Entity.class, o2_1_id ); } );
		ListBiowning2Entity o2_2 = inTransaction( em -> { return em.find( ListBiowning2Entity.class, o2_2_id ); } );

		ListBiowning1Entity rev1 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 1 );
		ListBiowning1Entity rev2 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 2 );
		ListBiowning1Entity rev3 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 3 );
		ListBiowning1Entity rev4 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 4 );
		ListBiowning1Entity rev5 = getAuditReader().find( ListBiowning1Entity.class, o1_1_id, 5 );

		assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReferences(), containsInAnyOrder( o2_1 ) );
		assertThat( rev3.getReferences(), containsInAnyOrder( o2_1, o2_2 ) );
		assertThat( rev4.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev5.getReferences(), containsInAnyOrder( o2_2 ) );
	}

	@DynamicTest
	public void testHistoryOfO1_2() {
		ListBiowning2Entity o2_1 = inTransaction( em -> { return em.find( ListBiowning2Entity.class, o2_1_id ); } );
		ListBiowning2Entity o2_2 = inTransaction( em -> { return em.find( ListBiowning2Entity.class, o2_2_id ); } );

		ListBiowning1Entity rev1 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 1 );
		ListBiowning1Entity rev2 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 2 );
		ListBiowning1Entity rev3 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 3 );
		ListBiowning1Entity rev4 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 4 );
		ListBiowning1Entity rev5 = getAuditReader().find( ListBiowning1Entity.class, o1_2_id, 5 );

		assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReferences(), containsInAnyOrder( o2_2 ) );
		assertThat( rev3.getReferences(), containsInAnyOrder( o2_2 ) );
		assertThat( rev4.getReferences(), containsInAnyOrder( o2_1, o2_2 ) );
		assertThat( rev5.getReferences(), containsInAnyOrder( o2_2 ) );
	}

	@DynamicTest
	public void testHistoryOfO2_1() {
		ListBiowning1Entity o1_1 = inTransaction( em -> { return em.find( ListBiowning1Entity.class, o1_1_id ); } );
		ListBiowning1Entity o1_2 = inTransaction( em -> { return em.find( ListBiowning1Entity.class, o1_2_id ); } );

		ListBiowning2Entity rev1 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 1 );
		ListBiowning2Entity rev2 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 2 );
		ListBiowning2Entity rev3 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 3 );
		ListBiowning2Entity rev4 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 4 );
		ListBiowning2Entity rev5 = getAuditReader().find( ListBiowning2Entity.class, o2_1_id, 5 );

		assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReferences(), containsInAnyOrder( o1_1 ) );
		assertThat( rev3.getReferences(), containsInAnyOrder( o1_1 ) );
		assertThat( rev4.getReferences(), containsInAnyOrder( o1_2 ) );
		assertThat( rev5.getReferences(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfO2_2() {
		ListBiowning1Entity o1_1 = inTransaction( em -> { return em.find( ListBiowning1Entity.class, o1_1_id ); } );
		ListBiowning1Entity o1_2 = inTransaction( em -> { return em.find( ListBiowning1Entity.class, o1_2_id ); } );

		ListBiowning2Entity rev1 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 1 );
		ListBiowning2Entity rev2 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 2 );
		ListBiowning2Entity rev3 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 3 );
		ListBiowning2Entity rev4 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 4 );
		ListBiowning2Entity rev5 = getAuditReader().find( ListBiowning2Entity.class, o2_2_id, 5 );

		assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getReferences(), containsInAnyOrder( o1_2 ) );
		assertThat( rev3.getReferences(), containsInAnyOrder( o1_1, o1_2 ) );
		assertThat( rev4.getReferences(), containsInAnyOrder( o1_2 ) );
		assertThat( rev5.getReferences(), containsInAnyOrder( o1_1, o1_2 ) );
	}
}