/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntityComparator;
import org.hibernate.orm.test.envers.entities.manytomany.SortedSetEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, SortedSetEntity.class})
public class CustomComparatorEntityTest {

	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			SortedSetEntity entity1 = new SortedSetEntity( 1, "sortedEntity1" );
			em.persist( entity1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SortedSetEntity entity1 = em.find( SortedSetEntity.class, 1 );
			final StrTestEntity strTestEntity1 = new StrTestEntity( "abc" );
			em.persist( strTestEntity1 );
			id1 = strTestEntity1.getId();
			entity1.getSortedSet().add( strTestEntity1 );
			entity1.getSortedMap().put( strTestEntity1, "abc" );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			SortedSetEntity entity1 = em.find( SortedSetEntity.class, 1 );
			final StrTestEntity strTestEntity2 = new StrTestEntity( "aaa" );
			em.persist( strTestEntity2 );
			id2 = strTestEntity2.getId();
			entity1.getSortedSet().add( strTestEntity2 );
			entity1.getSortedMap().put( strTestEntity2, "aaa" );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SortedSetEntity entity1 = em.find( SortedSetEntity.class, 1 );
			final StrTestEntity strTestEntity3 = new StrTestEntity( "aba" );
			em.persist( strTestEntity3 );
			id3 = strTestEntity3.getId();
			entity1.getSortedSet().add( strTestEntity3 );
			entity1.getSortedMap().put( strTestEntity3, "aba" );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			SortedSetEntity entity1 = em.find( SortedSetEntity.class, 1 );
			final StrTestEntity strTestEntity4 = new StrTestEntity( "aac" );
			em.persist( strTestEntity4 );
			id4 = strTestEntity4.getId();
			entity1.getSortedSet().add( strTestEntity4 );
			entity1.getSortedMap().put( strTestEntity4, "aac" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( SortedSetEntity.class, 1 ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( StrTestEntity.class, id1 ) );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( StrTestEntity.class, id2 ) );
			assertEquals( Arrays.asList( 4 ), auditReader.getRevisions( StrTestEntity.class, id3 ) );
			assertEquals( Arrays.asList( 5 ), auditReader.getRevisions( StrTestEntity.class, id4 ) );
		} );
	}

	@Test
	public void testCurrentStateOfEntity1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final SortedSetEntity entity1 = em.find( SortedSetEntity.class, 1 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			final SortedSet<StrTestEntity> sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 4, sortedSet.size() );
			final Iterator<StrTestEntity> iterator = sortedSet.iterator();
			checkStrTestEntity( iterator.next(), id2, "aaa" );
			checkStrTestEntity( iterator.next(), id4, "aac" );
			checkStrTestEntity( iterator.next(), id3, "aba" );
			checkStrTestEntity( iterator.next(), id1, "abc" );

			final SortedMap<StrTestEntity, String> sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 4, sortedMap.size() );
			Iterator<Map.Entry<StrTestEntity, String>> mapIterator = sortedMap.entrySet().iterator();
			checkStrTestEntity( mapIterator.next().getKey(), id2, "aaa" );
			checkStrTestEntity( mapIterator.next().getKey(), id4, "aac" );
			checkStrTestEntity( mapIterator.next().getKey(), id3, "aba" );
			checkStrTestEntity( mapIterator.next().getKey(), id1, "abc" );

			mapIterator = sortedMap.entrySet().iterator();
			assertEquals( mapIterator.next().getValue(), "aaa" );
			assertEquals( mapIterator.next().getValue(), "aac" );
			assertEquals( mapIterator.next().getValue(), "aba" );
			assertEquals( mapIterator.next().getValue(), "abc" );
		} );
	}

	private void checkStrTestEntity(StrTestEntity entity, Integer id, String sortKey) {
		assertEquals( id, entity.getId() );
		assertEquals( sortKey, entity.getStr() );
	}

	@Test
	public void testHistoryOfEntity1(EntityManagerFactoryScope scope) throws Exception {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			SortedSetEntity entity1 = auditReader.find( SortedSetEntity.class, 1, 1 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			SortedSet<StrTestEntity> sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 0, sortedSet.size() );

			SortedMap<StrTestEntity, String> sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 0, sortedMap.size() );

			entity1 = auditReader.find( SortedSetEntity.class, 1, 2 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 1, sortedSet.size() );
			Iterator<StrTestEntity> iterator = sortedSet.iterator();
			checkStrTestEntity( iterator.next(), id1, "abc" );

			sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 1, sortedMap.size() );
			Iterator<Map.Entry<StrTestEntity, String>> mapIterator = sortedMap.entrySet().iterator();
			checkStrTestEntity( mapIterator.next().getKey(), id1, "abc" );

			mapIterator = sortedMap.entrySet().iterator();
			assertEquals( mapIterator.next().getValue(), "abc" );

			entity1 = auditReader.find( SortedSetEntity.class, 1, 3 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 2, sortedSet.size() );
			iterator = sortedSet.iterator();
			checkStrTestEntity( iterator.next(), id2, "aaa" );
			checkStrTestEntity( iterator.next(), id1, "abc" );

			sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 2, sortedMap.size() );
			mapIterator = sortedMap.entrySet().iterator();
			checkStrTestEntity( mapIterator.next().getKey(), id2, "aaa" );
			checkStrTestEntity( mapIterator.next().getKey(), id1, "abc" );

			mapIterator = sortedMap.entrySet().iterator();
			assertEquals( mapIterator.next().getValue(), "aaa" );
			assertEquals( mapIterator.next().getValue(), "abc" );

			entity1 = auditReader.find( SortedSetEntity.class, 1, 4 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 3, sortedSet.size() );
			iterator = sortedSet.iterator();
			checkStrTestEntity( iterator.next(), id2, "aaa" );
			checkStrTestEntity( iterator.next(), id3, "aba" );
			checkStrTestEntity( iterator.next(), id1, "abc" );

			sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 3, sortedMap.size() );
			mapIterator = sortedMap.entrySet().iterator();
			checkStrTestEntity( mapIterator.next().getKey(), id2, "aaa" );
			checkStrTestEntity( mapIterator.next().getKey(), id3, "aba" );
			checkStrTestEntity( mapIterator.next().getKey(), id1, "abc" );

			mapIterator = sortedMap.entrySet().iterator();
			assertEquals( mapIterator.next().getValue(), "aaa" );
			assertEquals( mapIterator.next().getValue(), "aba" );
			assertEquals( mapIterator.next().getValue(), "abc" );

			entity1 = auditReader.find( SortedSetEntity.class, 1, 5 );

			assertEquals( "sortedEntity1", entity1.getData() );
			assertEquals( Integer.valueOf( 1 ), entity1.getId() );

			sortedSet = entity1.getSortedSet();
			assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
			assertEquals( 4, sortedSet.size() );
			iterator = sortedSet.iterator();
			checkStrTestEntity( iterator.next(), id2, "aaa" );
			checkStrTestEntity( iterator.next(), id4, "aac" );
			checkStrTestEntity( iterator.next(), id3, "aba" );
			checkStrTestEntity( iterator.next(), id1, "abc" );

			sortedMap = entity1.getSortedMap();
			assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
			assertEquals( 4, sortedMap.size() );
			mapIterator = sortedMap.entrySet().iterator();
			checkStrTestEntity( mapIterator.next().getKey(), id2, "aaa" );
			checkStrTestEntity( mapIterator.next().getKey(), id4, "aac" );
			checkStrTestEntity( mapIterator.next().getKey(), id3, "aba" );
			checkStrTestEntity( mapIterator.next().getKey(), id1, "abc" );

			mapIterator = sortedMap.entrySet().iterator();
			assertEquals( mapIterator.next().getValue(), "aaa" );
			assertEquals( mapIterator.next().getValue(), "aac" );
			assertEquals( mapIterator.next().getValue(), "aba" );
			assertEquals( mapIterator.next().getValue(), "abc" );
		} );
	}

}
