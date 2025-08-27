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
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntityComparator;
import org.hibernate.orm.test.envers.entities.manytomany.SortedSetEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class CustomComparatorEntityTest extends BaseEnversJPAFunctionalTestCase {

	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, SortedSetEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SortedSetEntity entity1 = new SortedSetEntity( 1, "sortedEntity1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( entity1 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		entity1 = em.find( SortedSetEntity.class, 1 );
		final StrTestEntity strTestEntity1 = new StrTestEntity( "abc" );
		em.persist( strTestEntity1 );
		id1 = strTestEntity1.getId();
		entity1.getSortedSet().add( strTestEntity1 );
		entity1.getSortedMap().put( strTestEntity1, "abc" );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		entity1 = em.find( SortedSetEntity.class, 1 );
		final StrTestEntity strTestEntity2 = new StrTestEntity( "aaa" );
		em.persist( strTestEntity2 );
		id2 = strTestEntity2.getId();
		entity1.getSortedSet().add( strTestEntity2 );
		entity1.getSortedMap().put( strTestEntity2, "aaa" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		entity1 = em.find( SortedSetEntity.class, 1 );
		final StrTestEntity strTestEntity3 = new StrTestEntity( "aba" );
		em.persist( strTestEntity3 );
		id3 = strTestEntity3.getId();
		entity1.getSortedSet().add( strTestEntity3 );
		entity1.getSortedMap().put( strTestEntity3, "aba" );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		entity1 = em.find( SortedSetEntity.class, 1 );
		final StrTestEntity strTestEntity4 = new StrTestEntity( "aac" );
		em.persist( strTestEntity4 );
		id4 = strTestEntity4.getId();
		entity1.getSortedSet().add( strTestEntity4 );
		entity1.getSortedMap().put( strTestEntity4, "aac" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), getAuditReader().getRevisions( SortedSetEntity.class, 1 ) );
		assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( StrTestEntity.class, id1 ) );
		assertEquals( Arrays.asList( 3 ), getAuditReader().getRevisions( StrTestEntity.class, id2 ) );
		assertEquals( Arrays.asList( 4 ), getAuditReader().getRevisions( StrTestEntity.class, id3 ) );
		assertEquals( Arrays.asList( 5 ), getAuditReader().getRevisions( StrTestEntity.class, id4 ) );
	}

	@Test
	public void testCurrentStateOfEntity1() {
		final SortedSetEntity entity1 = getEntityManager().find( SortedSetEntity.class, 1 );

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
	}

	private void checkStrTestEntity(StrTestEntity entity, Integer id, String sortKey) {
		assertEquals( id, entity.getId() );
		assertEquals( sortKey, entity.getStr() );
	}

	@Test
	public void testHistoryOfEntity1() throws Exception {
		SortedSetEntity entity1 = getAuditReader().find( SortedSetEntity.class, 1, 1 );

		assertEquals( "sortedEntity1", entity1.getData() );
		assertEquals( Integer.valueOf( 1 ), entity1.getId() );

		SortedSet<StrTestEntity> sortedSet = entity1.getSortedSet();
		assertEquals( StrTestEntityComparator.class, sortedSet.comparator().getClass() );
		assertEquals( 0, sortedSet.size() );

		SortedMap<StrTestEntity, String> sortedMap = entity1.getSortedMap();
		assertEquals( StrTestEntityComparator.class, sortedMap.comparator().getClass() );
		assertEquals( 0, sortedMap.size() );

		entity1 = getAuditReader().find( SortedSetEntity.class, 1, 2 );

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

		entity1 = getAuditReader().find( SortedSetEntity.class, 1, 3 );

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

		entity1 = getAuditReader().find( SortedSetEntity.class, 1, 4 );

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

		entity1 = getAuditReader().find( SortedSetEntity.class, 1, 5 );

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
	}

}
