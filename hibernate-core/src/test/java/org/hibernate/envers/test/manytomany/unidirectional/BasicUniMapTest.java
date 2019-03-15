/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.MapUniEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, MapUniEntity.class };
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
				// Revision 1  (coll1: initially one mapping)
				entityManager -> {
					final StrTestEntity str1 = new StrTestEntity( "str1" );
					final StrTestEntity str2 = new StrTestEntity( "str2" );

					entityManager.persist( str1 );
					entityManager.persist( str2 );

					final MapUniEntity coll1 = new MapUniEntity( 3, "coll1" );

					coll1.setMap( new HashMap<>() );
					coll1.getMap().put( "1", str1 );
					entityManager.persist( coll1 );


					str1_id = str1.getId();
					str2_id = str2.getId();

					coll1_id = coll1.getId();
				},

				// Revision 2 (coll1: adding one mapping)
				entityManager -> {
					final StrTestEntity str2 = entityManager.find( StrTestEntity.class, str2_id );
					final MapUniEntity coll1 = entityManager.find( MapUniEntity.class, coll1_id );
					coll1.getMap().put( "2", str2 );
				},

				// Revision 3 (coll1: replacing one mapping)
				entityManager -> {
					final StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
					final MapUniEntity coll1 = entityManager.find( MapUniEntity.class, coll1_id );
					coll1.getMap().put( "2", str1 );
				},

				// Revision 4 (coll1: removing one mapping)
				entityManager -> {
					final MapUniEntity coll1 = entityManager.find( MapUniEntity.class, coll1_id );
					coll1.getMap().remove( "1" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( MapUniEntity.class, coll1_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfColl1() {
		inTransaction(
				entityManager -> {
					StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
					StrTestEntity str2 = entityManager.find( StrTestEntity.class, str2_id );

					MapUniEntity rev1 = getAuditReader().find( MapUniEntity.class, coll1_id, 1 );
					MapUniEntity rev2 = getAuditReader().find( MapUniEntity.class, coll1_id, 2 );
					MapUniEntity rev3 = getAuditReader().find( MapUniEntity.class, coll1_id, 3 );
					MapUniEntity rev4 = getAuditReader().find( MapUniEntity.class, coll1_id, 4 );

					assertThat( rev1.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev1.getMap(), hasEntry( "1", str1 ) );

					assertThat( rev2.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev2.getMap(), hasEntry( "1", str1 ) );
					assertThat( rev2.getMap(), hasEntry( "2", str2 ) );

					assertThat( rev3.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev3.getMap(), hasEntry( "1", str1 ) );
					assertThat( rev3.getMap(), hasEntry( "2", str1 ) );

					assertThat( rev4.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev4.getMap(), hasEntry( "2", str1 ) );

					assertThat( rev1.getData(), equalTo( "coll1" ) );
					assertThat( rev2.getData(), equalTo( "coll1" ) );
					assertThat( rev3.getData(), equalTo( "coll1" ) );
					assertThat( rev4.getData(), equalTo( "coll1" ) );
				}
		);
	}
}