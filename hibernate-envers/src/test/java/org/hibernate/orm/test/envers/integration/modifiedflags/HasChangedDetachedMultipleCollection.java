/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionEntity;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity1;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity2;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7437")
@SkipForDialect(value = OracleDialect.class,
		comment = "Oracle does not support identity key generation")
@SkipForDialect(value = AltibaseDialect.class,
		comment = "Altibase does not support identity key generation")
public class HasChangedDetachedMultipleCollection extends AbstractModifiedFlagsEntityTest {
	private Long mce1Id = null;
	private Long mce2Id = null;
	private Long mcre1Id = null;
	private Long mcre2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MultipleCollectionEntity.class, MultipleCollectionRefEntity1.class, MultipleCollectionRefEntity2.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - addition.
		em.getTransaction().begin();
		MultipleCollectionEntity mce1 = new MultipleCollectionEntity();
		mce1.setText( "MultipleCollectionEntity-1-1" );
		em.persist( mce1 ); // Persisting entity with empty collections.
		em.getTransaction().commit();

		mce1Id = mce1.getId();

		// Revision 2 - update.
		em.getTransaction().begin();
		mce1 = em.find( MultipleCollectionEntity.class, mce1.getId() );
		MultipleCollectionRefEntity1 mcre1 = new MultipleCollectionRefEntity1();
		mcre1.setText( "MultipleCollectionRefEntity1-1-1" );
		mcre1.setMultipleCollectionEntity( mce1 );
		mce1.addRefEntity1( mcre1 );
		em.persist( mcre1 );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		mcre1Id = mcre1.getId();

		// No changes.
		em.getTransaction().begin();
		mce1 = em.find( MultipleCollectionEntity.class, mce1.getId() );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// Revision 3 - updating detached collection.
		em.getTransaction().begin();
		mce1.removeRefEntity1( mcre1 );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// Revision 4 - updating detached entity, no changes to collection attributes.
		em.getTransaction().begin();
		mce1.setRefEntities1( new ArrayList<MultipleCollectionRefEntity1>() );
		mce1.setRefEntities2( new ArrayList<MultipleCollectionRefEntity2>() );
		mce1.setText( "MultipleCollectionEntity-1-2" );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager();

		// No changes to detached entity (collections were empty before).
		em.getTransaction().begin();
		mce1.setRefEntities1( new ArrayList<MultipleCollectionRefEntity1>() );
		mce1.setRefEntities2( new ArrayList<MultipleCollectionRefEntity2>() );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		// Revision 5 - addition.
		em.getTransaction().begin();
		MultipleCollectionEntity mce2 = new MultipleCollectionEntity();
		mce2.setText( "MultipleCollectionEntity-2-1" );
		MultipleCollectionRefEntity2 mcre2 = new MultipleCollectionRefEntity2();
		mcre2.setText( "MultipleCollectionRefEntity2-1-1" );
		mcre2.setMultipleCollectionEntity( mce2 );
		mce2.addRefEntity2( mcre2 );
		em.persist( mce2 ); // Cascade persisting related entity.
		em.getTransaction().commit();

		mce2Id = mce2.getId();
		mcre2Id = mcre2.getId();

		em.close();
	}

	@Test
	public void testHasChanged() {
		List list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "text" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "refEntities1" );
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "refEntities2" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionRefEntity1.class, mcre1Id, "text" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce2Id, "text" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce2Id, "refEntities2" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( MultipleCollectionRefEntity2.class, mcre2Id, "text" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );
	}
}
