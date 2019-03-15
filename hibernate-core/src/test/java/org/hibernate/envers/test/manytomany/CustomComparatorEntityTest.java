/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.SortedSetEntity;
import org.hibernate.envers.test.support.domains.manytomany.StrTestEntityComparator;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class CustomComparatorEntityTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, SortedSetEntity.class };
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
					final SortedSetEntity entity = new SortedSetEntity( 1, "sortedEntity1" );
					entityManager.persist( entity );
				},

				// Revision 2
				entityManager -> {
					final SortedSetEntity entity = entityManager.find( SortedSetEntity.class, 1 );

					final StrTestEntity strTestEntity = new StrTestEntity( "abc" );
					entityManager.persist( strTestEntity );
					this.id1 = strTestEntity.getId();

					entity.getSortedSet().add( strTestEntity );
					entity.getSortedMap().put( strTestEntity, "abc" );
				},

				// Revision 3
				entityManager -> {
					final SortedSetEntity entity = entityManager.find( SortedSetEntity.class, 1 );

					final StrTestEntity strTestEntity = new StrTestEntity( "aaa" );
					entityManager.persist( strTestEntity );
					this.id2 = strTestEntity.getId();

					entity.getSortedSet().add( strTestEntity );
					entity.getSortedMap().put( strTestEntity, "aaa" );
				},

				// Revision 4
				entityManager -> {
					final SortedSetEntity entity = entityManager.find( SortedSetEntity.class, 1 );

					final StrTestEntity strTestEntity = new StrTestEntity( "aba" );
					entityManager.persist( strTestEntity );
					this.id3 = strTestEntity.getId();

					entity.getSortedSet().add( strTestEntity );
					entity.getSortedMap().put( strTestEntity, "aba" );
				},

				// Revision 5
				entityManager -> {
					final SortedSetEntity entity = entityManager.find( SortedSetEntity.class, 1 );

					final StrTestEntity strTestEntity = new StrTestEntity( "aac" );
					entityManager.persist( strTestEntity );
					this.id4 = strTestEntity.getId();

					entity.getSortedSet().add( strTestEntity );
					entity.getSortedMap().put( strTestEntity, "aac" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SortedSetEntity.class, 1 ), contains( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id1 ), contains( 2 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id2 ), contains( 3 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id3 ), contains( 4 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id4 ), contains( 5 ) );
	}

	@DynamicTest
	public void testCurrentStateOfEntity1() {
		inTransaction(
				entityManager -> {
					final SortedSetEntity sortedSetEntity = entityManager.find( SortedSetEntity.class, 1 );
					assertThat( sortedSetEntity.getData(), equalTo( "sortedEntity1" ) );
					assertThat( sortedSetEntity.getId(), equalTo( 1 ) );

					final SortedSet<StrTestEntity> sortedSet = sortedSetEntity.getSortedSet();
					assertThat( sortedSet.comparator(), instanceOf( StrTestEntityComparator.class ) );
					assertThat(
							sortedSet,
							contains(
									new StrTestEntity( id2, "aaa" ),
									new StrTestEntity( id4, "aac" ),
									new StrTestEntity( id3, "aba" ),
									new StrTestEntity( id1, "abc" )
							)
					);

					final SortedMap<StrTestEntity, String> sortedMap = sortedSetEntity.getSortedMap();
					assertThat( sortedMap.comparator(), instanceOf( StrTestEntityComparator.class ) );
					assertThat(
							sortedMap.keySet(),
							contains(
									new StrTestEntity( id2, "aaa" ),
									new StrTestEntity( id4, "aac" ),
									new StrTestEntity( id3, "aba" ),
									new StrTestEntity( id1, "abc" )
							)
					);
					assertThat( sortedMap.values(), contains( "aaa", "aac", "aba", "abc" ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		final StrTestEntity str1 = new StrTestEntity( id1, "abc" );
		final StrTestEntity str2 = new StrTestEntity( id2, "aaa" );
		final StrTestEntity str3 = new StrTestEntity( id3, "aba" );
		final StrTestEntity str4 = new StrTestEntity( id4, "aac" );

		// Revision 1
		final SortedSetEntity rev1 = getAuditReader().find( SortedSetEntity.class, 1, 1 );
		assertThat( rev1.getData(), equalTo( "sortedEntity1" ) );
		assertThat( rev1.getId(), equalTo( 1 ) );
		assertThatSetContains( rev1.getSortedSet() );
		assertThatMapContains( rev1.getSortedMap() );

		// Revision 2
		final SortedSetEntity rev2 = getAuditReader().find( SortedSetEntity.class, 1, 2 );
		assertThat( rev2.getData(), equalTo( "sortedEntity1" ) );
		assertThat( rev2.getId(), equalTo( 1 ) );
		assertThatSetContains( rev2.getSortedSet(), str1 );
		assertThatMapContains( rev2.getSortedMap(), str1 );

		// Revision 3
		final SortedSetEntity rev3 = getAuditReader().find( SortedSetEntity.class, 1, 3 );
		assertThat( rev3.getData(), equalTo( "sortedEntity1" ) );
		assertThat( rev3.getId(), equalTo( 1 ) );
		assertThatSetContains( rev3.getSortedSet(), str2, str1 );
		assertThatMapContains( rev3.getSortedMap(), str2, str1 );

		// Revision 4
		final SortedSetEntity rev4 = getAuditReader().find( SortedSetEntity.class, 1, 4 );
		assertThat( rev4.getData(), equalTo( "sortedEntity1" ) );
		assertThat( rev4.getId(), equalTo( 1 ) );
		assertThatSetContains( rev4.getSortedSet(), str2, str3, str1 );
		assertThatMapContains( rev4.getSortedMap(), str2, str3, str1 );

		// Revision 5
		final SortedSetEntity rev5 = getAuditReader().find( SortedSetEntity.class, 1, 5 );
		assertThat( rev5.getData(), equalTo( "sortedEntity1" ) );
		assertThat( rev5.getId(), equalTo( 1 ) );
		assertThatSetContains( rev5.getSortedSet(), str2, str4, str3, str1 );
		assertThatMapContains( rev5.getSortedMap(), str2, str4, str3, str1 );
	}

	private static void assertThatSetContains(SortedSet<StrTestEntity> collection, StrTestEntity... elements) {
		assertThat( collection.comparator(), instanceOf( StrTestEntityComparator.class ) );
		if ( elements.length == 0 ) {
			assertThat( collection, CollectionMatchers.isEmpty() );
		}
		else {
			assertThat( collection, contains( elements ) );
		}
	}

	private static void assertThatMapContains(SortedMap<StrTestEntity, String> collection, StrTestEntity... elements) {
		assertThat( collection.comparator(), instanceOf( StrTestEntityComparator.class ) );
		if ( elements.length == 0 ) {
			assertThat( collection.entrySet(), CollectionMatchers.isEmpty() );
		}
		else {
			assertThat( collection.keySet(), contains( elements ) );
			assertThat( collection.values(), contains( extractStringValues( elements ).toArray( new String[ 0 ] ) ) );
		}
	}

	private static List<String> extractStringValues(StrTestEntity... entities) {
		return Arrays.asList( entities ).stream().map( StrTestEntity::getStr ).collect( Collectors.toList() );
	}
}
