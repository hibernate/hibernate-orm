/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionEntity;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity1;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity2;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7437")
@SkipForDialect(dialectClass = OracleDialect.class,
		reason = "Oracle does not support identity key generation")
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "Altibase does not support identity key generation")
@EnversTest
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {MultipleCollectionEntity.class, MultipleCollectionRefEntity1.class, MultipleCollectionRefEntity2.class})
public class HasChangedDetachedMultipleCollection extends AbstractModifiedFlagsEntityTest {
	private Long mce1Id = null;
	private Long mce2Id = null;
	private Long mcre1Id = null;
	private Long mcre2Id = null;

	MultipleCollectionEntity mce1;
	MultipleCollectionRefEntity1 mcre1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1 - addition.
			em.getTransaction().begin();
			mce1 = new MultipleCollectionEntity();
			mce1.setText( "MultipleCollectionEntity-1-1" );
			em.persist( mce1 ); // Persisting entity with empty collections.
			em.getTransaction().commit();

			mce1Id = mce1.getId();

			// Revision 2 - update.
			em.getTransaction().begin();
			mce1 = em.find( MultipleCollectionEntity.class, mce1.getId() );
			mcre1 = new MultipleCollectionRefEntity1();
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
		} );

		scope.inEntityManager( em -> {
			// Revision 3 - updating detached collection.
			em.getTransaction().begin();
			mce1.removeRefEntity1( mcre1 );
			mce1 = em.merge( mce1 );
			em.getTransaction().commit();
		} );

		scope.inEntityManager( em -> {
			// Revision 4 - updating detached entity, no changes to collection attributes.
			em.getTransaction().begin();
			mce1.setRefEntities1( new ArrayList<>() );
			mce1.setRefEntities2( new ArrayList<>() );
			mce1.setText( "MultipleCollectionEntity-1-2" );
			mce1 = em.merge( mce1 );
			em.getTransaction().commit();
		} );

		scope.inEntityManager( em -> {
			// No changes to detached entity (collections were empty before).
			em.getTransaction().begin();
			mce1.setRefEntities1( new ArrayList<>() );
			mce1.setRefEntities2( new ArrayList<>() );
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
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, MultipleCollectionEntity.class, mce1Id, "text" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionEntity.class, mce1Id, "refEntities1" );
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionEntity.class, mce1Id, "refEntities2" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionRefEntity1.class, mcre1Id, "text" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionEntity.class, mce2Id, "text" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionEntity.class, mce2Id, "refEntities2" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, MultipleCollectionRefEntity2.class, mcre2Id, "text" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 5 ), extractRevisionNumbers( list ) );
		} );
	}
}
