/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.SetUniEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetUniEntity.class, StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				em -> {
					final StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
					final StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

					final SetUniEntity ing1 = new SetUniEntity( 3, "data_ing_1" );
					final SetUniEntity ing2 = new SetUniEntity( 4, "data_ing_2" );

					em.persist( ed1 );
					em.persist( ed2 );
					em.persist( ing1 );
					em.persist( ing2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				em -> {
					final SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
					final SetUniEntity ing2 = em.find( SetUniEntity.class, ing2_id );
					final StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
					final StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

					ing1.setReferences( new HashSet<>() );
					ing1.getReferences().add( ed1 );

					ing2.setReferences( new HashSet<>() );
					ing2.getReferences().add( ed1 );
					ing2.getReferences().add( ed2 );
				},

				// Revision 3
				em -> {
					final SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
					final StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );
					final StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
					ing1.getReferences().add( ed2 );
				},

				// Revision 4
				em -> {
					final SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
					final StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );
					final StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
					ing1.getReferences().remove( ed1 );
				},

				// Revision 5
				em -> {
					final SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
					ing1.setReferences( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, ed2_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( SetUniEntity.class, ing1_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( SetUniEntity.class, ing2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );

					SetUniEntity rev1 = getAuditReader().find( SetUniEntity.class, ing1_id, 1 );
					SetUniEntity rev2 = getAuditReader().find( SetUniEntity.class, ing1_id, 2 );
					SetUniEntity rev3 = getAuditReader().find( SetUniEntity.class, ing1_id, 3 );
					SetUniEntity rev4 = getAuditReader().find( SetUniEntity.class, ing1_id, 4 );
					SetUniEntity rev5 = getAuditReader().find( SetUniEntity.class, ing1_id, 5 );

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), containsInAnyOrder( ed2 ) );
					assertThat( rev5.getReferences(), CollectionMatchers.isEmpty() );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );

					SetUniEntity rev1 = getAuditReader().find( SetUniEntity.class, ing2_id, 1 );
					SetUniEntity rev2 = getAuditReader().find( SetUniEntity.class, ing2_id, 2 );
					SetUniEntity rev3 = getAuditReader().find( SetUniEntity.class, ing2_id, 3 );
					SetUniEntity rev4 = getAuditReader().find( SetUniEntity.class, ing2_id, 4 );
					SetUniEntity rev5 = getAuditReader().find( SetUniEntity.class, ing2_id, 5 );

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev3.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev4.getReferences(), containsInAnyOrder( ed1, ed2 ) );
					assertThat( rev5.getReferences(), containsInAnyOrder( ed1, ed2 ) );
				}
		);
	}
}