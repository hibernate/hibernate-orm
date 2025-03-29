/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.PartialModifiedFlagsEntity;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.WithModifiedFlagReferencingEntity;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedForDefaultNotUsing extends AbstractModifiedFlagsEntityTest {
	private static final int entityId = 1;
	private static final int refEntityId = 1;

	@Override
	public boolean forceModifiedFlags() {
		return false;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				PartialModifiedFlagsEntity.class,
				WithModifiedFlagReferencingEntity.class,
				StrTestEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {

		PartialModifiedFlagsEntity entity =
				new PartialModifiedFlagsEntity( entityId );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( entity );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		entity.setData( "data1" );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		entity.setComp1( new Component1( "str1", "str2" ) );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		entity.setComp2( new Component2( "str1", "str2" ) );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		WithModifiedFlagReferencingEntity withModifiedFlagReferencingEntity = new WithModifiedFlagReferencingEntity(
				refEntityId,
				"first"
		);
		withModifiedFlagReferencingEntity.setReference( entity );
		em.persist( withModifiedFlagReferencingEntity );

		em.getTransaction().commit();

		// Revision 6
		em.getTransaction().begin();

		withModifiedFlagReferencingEntity = em.find( WithModifiedFlagReferencingEntity.class, refEntityId );
		withModifiedFlagReferencingEntity.setReference( null );
		withModifiedFlagReferencingEntity.setSecondReference( entity );
		em.merge( withModifiedFlagReferencingEntity );

		em.getTransaction().commit();

		// Revision 7
		em.getTransaction().begin();

		entity.getStringSet().add( "firstElement" );
		entity.getStringSet().add( "secondElement" );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 8
		em.getTransaction().begin();

		entity.getStringSet().remove( "secondElement" );
		entity.getStringMap().put( "someKey", "someValue" );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 9 - main entity doesn't change
		em.getTransaction().begin();

		StrTestEntity strTestEntity = new StrTestEntity( "first" );
		em.persist( strTestEntity );

		em.getTransaction().commit();

		// Revision 10
		em.getTransaction().begin();

		entity.getEntitiesSet().add( strTestEntity );
		entity = em.merge( entity );

		em.getTransaction().commit();

		// Revision 11
		em.getTransaction().begin();

		entity.getEntitiesSet().remove( strTestEntity );
		entity.getEntitiesMap().put( "someKey", strTestEntity );
		em.merge( entity );

		em.getTransaction().commit();

		// Revision 12 - main entity doesn't change
		em.getTransaction().begin();

		strTestEntity.setStr( "second" );
		em.merge( strTestEntity );

		em.getTransaction().commit();

	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( (Number) 1, 2, 3, 4, 5, 6, 7, 8, 10, 11 ),
				getAuditReader()
						.getRevisions(
								PartialModifiedFlagsEntity.class,
								entityId
						)
		);
	}

	@Test
	public void testHasChangedData() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "data"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedComp1() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "comp1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasChangedComp2() throws Exception {
		queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "comp2"
		);
	}

	@Test
	public void testHasChangedReferencing() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "referencing"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 5, 6 ), extractRevisionNumbers( list ) );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasChangedReferencing2() throws Exception {
		queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "referencing2"
		);
	}

	@Test
	public void testHasChangedStringSet() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "stringSet"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 7, 8 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedStringMap() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "stringMap"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 8 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedStringSetAndMap() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "stringSet", "stringMap"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 8 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedEntitiesSet() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "entitiesSet"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 10, 11 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedEntitiesMap() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "entitiesMap"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 11 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedEntitiesSetAndMap() throws Exception {
		List list = queryForPropertyHasChanged(
				PartialModifiedFlagsEntity.class,
				entityId, "entitiesSet", "entitiesMap"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 11 ), extractRevisionNumbers( list ) );
	}

}
