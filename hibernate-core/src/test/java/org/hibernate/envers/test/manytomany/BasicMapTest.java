/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.MapOwnedEntity;
import org.hibernate.envers.test.support.domains.manytomany.MapOwningEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MapOwningEntity.class, MapOwnedEntity.class };
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
		inJPA(
				entityManager -> {
					MapOwnedEntity ed1 = new MapOwnedEntity( 1, "data_ed_1" );
					MapOwnedEntity ed2 = new MapOwnedEntity( 2, "data_ed_2" );

					MapOwningEntity ing1 = new MapOwningEntity( 3, "data_ing_1" );
					MapOwningEntity ing2 = new MapOwningEntity( 4, "data_ing_2" );

					// Revision 1 (ing1: initialy empty, ing2: one mapping)
					entityManager.getTransaction().begin();

					ing2.getReferences().put( "2", ed2 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					entityManager.getTransaction().commit();

					// Revision 2 (ing1: adding two mappings, ing2: replacing an existing mapping)

					entityManager.getTransaction().begin();

					ing1 = entityManager.find( MapOwningEntity.class, ing1.getId() );
					ing2 = entityManager.find( MapOwningEntity.class, ing2.getId() );
					ed1 = entityManager.find( MapOwnedEntity.class, ed1.getId() );
					ed2 = entityManager.find( MapOwnedEntity.class, ed2.getId() );

					ing1.getReferences().put( "1", ed1 );
					ing1.getReferences().put( "2", ed1 );

					ing2.getReferences().put( "2", ed1 );

					entityManager.getTransaction().commit();

					// No revision (ing1: adding an existing mapping, ing2: removing a non existing mapping)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( MapOwningEntity.class, ing1.getId() );
					ing2 = entityManager.find( MapOwningEntity.class, ing2.getId() );

					ing1.getReferences().put( "1", ed1 );

					ing2.getReferences().remove( "3" );

					entityManager.getTransaction().commit();

					// Revision 3 (ing1: clearing, ing2: replacing with a new map)
					entityManager.getTransaction().begin();

					ing1 = entityManager.find( MapOwningEntity.class, ing1.getId() );
					ed1 = entityManager.find( MapOwnedEntity.class, ed1.getId() );

					ing1.getReferences().clear();
					ing2.setReferences( new HashMap<String, MapOwnedEntity>() );
					ing2.getReferences().put( "1", ed2 );

					entityManager.getTransaction().commit();

					ed1_id = ed1.getId();
					ed2_id = ed2.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( MapOwnedEntity.class, ed1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( MapOwnedEntity.class, ed2_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( MapOwningEntity.class, ing1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( MapOwningEntity.class, ing2_id ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		inTransaction(
				entityManager -> {
					MapOwningEntity ing1 = entityManager.find( MapOwningEntity.class, ing1_id );
					MapOwningEntity ing2 = entityManager.find( MapOwningEntity.class, ing2_id );

					MapOwnedEntity rev1 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 1 );
					MapOwnedEntity rev2 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 2 );
					MapOwnedEntity rev3 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 3 );

					assertThat( rev1.getReferencing(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev2.getReferencing(), containsInAnyOrder( ing1, ing2 ) );
					assertThat( rev3.getReferencing(), equalTo( Collections.EMPTY_SET ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		inTransaction(
				entityManager -> {
					MapOwningEntity ing2 = entityManager.find( MapOwningEntity.class, ing2_id );

					MapOwnedEntity rev1 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 1 );
					MapOwnedEntity rev2 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 2 );
					MapOwnedEntity rev3 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 3 );

					assertThat( rev1.getReferencing(), contains( ing2 ) );
					assertThat( rev2.getReferencing(), equalTo( Collections.EMPTY_SET ) );
					assertThat( rev3.getReferencing(), contains( ing2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng1() {
		inTransaction(
				entityManager -> {
					MapOwnedEntity ed1 = entityManager.find( MapOwnedEntity.class, ed1_id );

					MapOwningEntity rev1 = getAuditReader().find( MapOwningEntity.class, ing1_id, 1 );
					MapOwningEntity rev2 = getAuditReader().find( MapOwningEntity.class, ing1_id, 2 );
					MapOwningEntity rev3 = getAuditReader().find( MapOwningEntity.class, ing1_id, 3 );

					assertThat( rev1.getReferences(), equalTo( Collections.EMPTY_MAP ) );

					assertThat( rev2.getReferences().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev2.getReferences(), hasEntry( "1", ed1 ) );
					assertThat( rev2.getReferences(), hasEntry( "2", ed1 ) );

					assertThat( rev3.getReferences(), equalTo( Collections.EMPTY_MAP ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEdIng2() {
		inTransaction(
				entityManager -> {
					MapOwnedEntity ed1 = entityManager.find( MapOwnedEntity.class, ed1_id );
					MapOwnedEntity ed2 = entityManager.find( MapOwnedEntity.class, ed2_id );

					MapOwningEntity rev1 = getAuditReader().find( MapOwningEntity.class, ing2_id, 1 );
					MapOwningEntity rev2 = getAuditReader().find( MapOwningEntity.class, ing2_id, 2 );
					MapOwningEntity rev3 = getAuditReader().find( MapOwningEntity.class, ing2_id, 3 );

					assertThat( rev1.getReferences().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev1.getReferences(), hasEntry( "2", ed2 ) );

					assertThat( rev2.getReferences().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev2.getReferences(), hasEntry( "2", ed1 ) );

					assertThat( rev3.getReferences().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev3.getReferences(), hasEntry( "1", ed2 ) );
				}
		);
	}
}