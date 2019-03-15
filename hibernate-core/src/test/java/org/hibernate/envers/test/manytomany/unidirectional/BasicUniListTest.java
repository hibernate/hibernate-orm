/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import java.util.ArrayList;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.ListUniEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ListUniEntity.class, StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
					final StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

					final ListUniEntity ing1 = new ListUniEntity( 3, "data_ing_1" );
					final ListUniEntity ing2 = new ListUniEntity( 4, "data_ing_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2
				entityManager -> {
					final ListUniEntity ing1 = entityManager.find( ListUniEntity.class, ing1_id );
					final ListUniEntity ing2 = entityManager.find( ListUniEntity.class, ing2_id );
					final StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					final StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );

					ing1.setReferences( new ArrayList<>() );
					ing1.getReferences().add( ed1 );

					ing2.setReferences( new ArrayList<>() );
					ing2.getReferences().add( ed1 );
					ing2.getReferences().add( ed2 );
				},

				// Revision 3
				entityManager -> {
					final ListUniEntity ing1 = entityManager.find( ListUniEntity.class, ing1_id );
					final StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );
					final StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					ing1.getReferences().add( ed2 );
				},

				// Revision 4
				entityManager -> {
					final ListUniEntity ing1 = entityManager.find( ListUniEntity.class, ing1_id );
					final StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );
					final StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					ing1.getReferences().remove( ed1 );
				},

				// Revision 5
				entityManager -> {
					final ListUniEntity ing1 = entityManager.find( ListUniEntity.class, ing1_id );
					ing1.setReferences( null );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, ed1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, ed2_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( ListUniEntity.class, ing1_id ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( ListUniEntity.class, ing2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					StrTestEntity ed1 = entityManager.find( StrTestEntity.class, ed1_id );
					StrTestEntity ed2 = entityManager.find( StrTestEntity.class, ed2_id );

					ListUniEntity rev1 = getAuditReader().find( ListUniEntity.class, ing1_id, 1 );
					ListUniEntity rev2 = getAuditReader().find( ListUniEntity.class, ing1_id, 2 );
					ListUniEntity rev3 = getAuditReader().find( ListUniEntity.class, ing1_id, 3 );
					ListUniEntity rev4 = getAuditReader().find( ListUniEntity.class, ing1_id, 4 );
					ListUniEntity rev5 = getAuditReader().find( ListUniEntity.class, ing1_id, 5 );

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), contains( ed1 ) );
					assertThat( rev3.getReferences(), contains( ed1, ed2 ) );
					assertThat( rev4.getReferences(), contains( ed2 ) );
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

					ListUniEntity rev1 = getAuditReader().find( ListUniEntity.class, ing2_id, 1 );
					ListUniEntity rev2 = getAuditReader().find( ListUniEntity.class, ing2_id, 2 );
					ListUniEntity rev3 = getAuditReader().find( ListUniEntity.class, ing2_id, 3 );
					ListUniEntity rev4 = getAuditReader().find( ListUniEntity.class, ing2_id, 4 );
					ListUniEntity rev5 = getAuditReader().find( ListUniEntity.class, ing2_id, 5 );

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), contains( ed1, ed2 ) );
					assertThat( rev3.getReferences(), contains( ed1, ed2 ) );
					assertThat( rev4.getReferences(), contains( ed1, ed2 ) );
					assertThat( rev5.getReferences(), contains( ed1, ed2 ) );
				}
		);
	}
}