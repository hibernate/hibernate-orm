/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.ternary;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestPrivSeqEntity;
import org.hibernate.envers.test.support.domains.basic.StrTestPrivSeqEntity;
import org.hibernate.envers.test.support.domains.manytomany.ternary.TernaryMapEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TernaryMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer str1_id;
	private Integer str2_id;

	private Integer int1_id;
	private Integer int2_id;

	private Integer map1_id;
	private Integer map2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TernaryMapEntity.class, StrTestPrivSeqEntity.class, IntTestPrivSeqEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {

					StrTestPrivSeqEntity str1 = new StrTestPrivSeqEntity( "a" );
					StrTestPrivSeqEntity str2 = new StrTestPrivSeqEntity( "b" );

					IntTestPrivSeqEntity int1 = new IntTestPrivSeqEntity( 1 );
					IntTestPrivSeqEntity int2 = new IntTestPrivSeqEntity( 2 );

					TernaryMapEntity map1 = new TernaryMapEntity();
					TernaryMapEntity map2 = new TernaryMapEntity();

					// Revision 1 (map1: initialy one mapping int1 -> str1, map2: empty)
					entityManager.getTransaction().begin();

					entityManager.persist( str1 );
					entityManager.persist( str2 );
					entityManager.persist( int1 );
					entityManager.persist( int2 );

					map1.getMap().put( int1, str1 );

					entityManager.persist( map1 );
					entityManager.persist( map2 );

					entityManager.getTransaction().commit();

					// Revision 2 (map1: replacing the mapping, map2: adding two mappings)
					entityManager.getTransaction().begin();

					map1 = entityManager.find( TernaryMapEntity.class, map1.getId() );
					map2 = entityManager.find( TernaryMapEntity.class, map2.getId() );

					str1 = entityManager.find( StrTestPrivSeqEntity.class, str1.getId() );
					str2 = entityManager.find( StrTestPrivSeqEntity.class, str2.getId() );

					int1 = entityManager.find( IntTestPrivSeqEntity.class, int1.getId() );
					int2 = entityManager.find( IntTestPrivSeqEntity.class, int2.getId() );

					map1.getMap().put( int1, str2 );

					map2.getMap().put( int1, str1 );
					map2.getMap().put( int2, str1 );

					entityManager.getTransaction().commit();

					// Revision 3 (map1: removing a non-existing mapping, adding an existing mapping - no changes, map2: removing a mapping)
					entityManager.getTransaction().begin();

					map1 = entityManager.find( TernaryMapEntity.class, map1.getId() );
					map2 = entityManager.find( TernaryMapEntity.class, map2.getId() );

					str2 = entityManager.find( StrTestPrivSeqEntity.class, str2.getId() );

					int1 = entityManager.find( IntTestPrivSeqEntity.class, int1.getId() );
					int2 = entityManager.find( IntTestPrivSeqEntity.class, int2.getId() );

					map1.getMap().remove( int2 );
					map1.getMap().put( int1, str2 );

					map2.getMap().remove( int1 );

					entityManager.getTransaction().commit();

					// Revision 4 (map1: adding a mapping, map2: adding a mapping)
					entityManager.getTransaction().begin();

					map1 = entityManager.find( TernaryMapEntity.class, map1.getId() );
					map2 = entityManager.find( TernaryMapEntity.class, map2.getId() );

					str2 = entityManager.find( StrTestPrivSeqEntity.class, str2.getId() );

					int1 = entityManager.find( IntTestPrivSeqEntity.class, int1.getId() );
					int2 = entityManager.find( IntTestPrivSeqEntity.class, int2.getId() );

					map1.getMap().put( int2, str2 );

					map2.getMap().put( int1, str2 );

					entityManager.getTransaction().commit();
					//

					map1_id = map1.getId();
					map2_id = map2.getId();

					str1_id = str1.getId();
					str2_id = str2.getId();

					int1_id = int1.getId();
					int2_id = int2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( TernaryMapEntity.class, map1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( TernaryMapEntity.class, map2_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( StrTestPrivSeqEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestPrivSeqEntity.class, str2_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( IntTestPrivSeqEntity.class, int1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( IntTestPrivSeqEntity.class, int2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfMap1() {
		inTransaction(
				entityManager -> {
					StrTestPrivSeqEntity str1 = entityManager.find( StrTestPrivSeqEntity.class, str1_id );
					StrTestPrivSeqEntity str2 = entityManager.find( StrTestPrivSeqEntity.class, str2_id );

					IntTestPrivSeqEntity int1 = entityManager.find( IntTestPrivSeqEntity.class, int1_id );
					IntTestPrivSeqEntity int2 = entityManager.find( IntTestPrivSeqEntity.class, int2_id );

					TernaryMapEntity rev1 = getAuditReader().find( TernaryMapEntity.class, map1_id, 1 );
					TernaryMapEntity rev2 = getAuditReader().find( TernaryMapEntity.class, map1_id, 2 );
					TernaryMapEntity rev3 = getAuditReader().find( TernaryMapEntity.class, map1_id, 3 );
					TernaryMapEntity rev4 = getAuditReader().find( TernaryMapEntity.class, map1_id, 4 );

					assertThat( rev1.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev1.getMap(), hasEntry( int1, str1 ) );

					assertThat( rev2.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev2.getMap(), hasEntry( int1, str2 ) );

					assertThat( rev3.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev3.getMap(), hasEntry( int1, str2 ) );

					assertThat( rev4.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev4.getMap(), hasEntry( int1, str2 ) );
					assertThat( rev4.getMap(), hasEntry( int2, str2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfMap2() {
		inTransaction(
				entityManager -> {
					StrTestPrivSeqEntity str1 = entityManager.find( StrTestPrivSeqEntity.class, str1_id );
					StrTestPrivSeqEntity str2 = entityManager.find( StrTestPrivSeqEntity.class, str2_id );

					IntTestPrivSeqEntity int1 = entityManager.find( IntTestPrivSeqEntity.class, int1_id );
					IntTestPrivSeqEntity int2 = entityManager.find( IntTestPrivSeqEntity.class, int2_id );

					TernaryMapEntity rev1 = getAuditReader().find( TernaryMapEntity.class, map2_id, 1 );
					TernaryMapEntity rev2 = getAuditReader().find( TernaryMapEntity.class, map2_id, 2 );
					TernaryMapEntity rev3 = getAuditReader().find( TernaryMapEntity.class, map2_id, 3 );
					TernaryMapEntity rev4 = getAuditReader().find( TernaryMapEntity.class, map2_id, 4 );

					assertThat( rev1.getMap().entrySet(), CollectionMatchers.isEmpty() );

					assertThat( rev2.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev2.getMap(), hasEntry( int1, str1 ) );
					assertThat( rev2.getMap(), hasEntry( int2, str1 ) );

					assertThat( rev3.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev3.getMap(), hasEntry( int2, str1 ) );

					assertThat( rev4.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev4.getMap(), hasEntry( int1, str2 ) );
					assertThat( rev4.getMap(), hasEntry( int2, str1 ) );
				}
		);
	}
}