/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged;
import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasNotChanged;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				DoubleListJoinColumnBidirectionalRefEdEntity2.class
		})
public class HasChangedDoubleJoinColumnBidirectionalList extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_1_id;
	private Integer ed2_1_id;
	private Integer ed1_2_id;
	private Integer ed2_2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void createData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
					"ed1_1",
					null
			);
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
					"ed1_2",
					null
			);

			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
					"ed2_1",
					null
			);
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
					"ed2_2",
					null
			);

			DoubleListJoinColumnBidirectionalRefIngEntity ing1 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll1" );
			DoubleListJoinColumnBidirectionalRefIngEntity ing2 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll2" );

			// Revision 1 (ing1: ed1_1, ed2_1, ing2: ed1_2, ed2_2)
			em.getTransaction().begin();

			ing1.getReferences1().add( ed1_1 );
			ing1.getReferences2().add( ed2_1 );

			ing2.getReferences1().add( ed1_2 );
			ing2.getReferences2().add( ed2_2 );

			em.persist( ed1_1 );
			em.persist( ed1_2 );
			em.persist( ed2_1 );
			em.persist( ed2_2 );
			em.persist( ing1 );
			em.persist( ing2 );

			em.getTransaction().commit();

			// Revision 2 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
			em.getTransaction().begin();

			DoubleListJoinColumnBidirectionalRefIngEntity ing1Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			DoubleListJoinColumnBidirectionalRefIngEntity ing2Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
			DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

			ing2Loaded.getReferences1().clear();
			ing2Loaded.getReferences2().clear();

			ing1Loaded.getReferences1().add( ed1_2Loaded );
			ing1Loaded.getReferences2().add( ed2_2Loaded );

			em.getTransaction().commit();
			em.clear();

			// Revision 3 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
			em.getTransaction().begin();

			ing1Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			ing2Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			ed1_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
			ed1_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
			ed2_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
			ed2_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

			ed1_1Loaded.setData( "ed1_1 bis" );
			ed2_2Loaded.setData( "ed2_2 bis" );

			em.getTransaction().commit();
			em.clear();

			// Revision 4 (ing1: ed2_2, ing2: ed2_1, ed1_1, ed1_2)
			em.getTransaction().begin();

			ing1Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
			ing2Loaded = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
			ed1_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
			ed1_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
			ed2_1Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
			ed2_2Loaded = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

			ing1Loaded.getReferences1().clear();
			ing2Loaded.getReferences1().add( ed1_1Loaded );
			ing2Loaded.getReferences1().add( ed1_2Loaded );

			ing1Loaded.getReferences2().remove( ed2_1Loaded );
			ing2Loaded.getReferences2().add( ed2_1Loaded );

			em.getTransaction().commit();
			em.clear();

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();

			ed1_1_id = ed1_1.getId();
			ed1_2_id = ed1_2.getId();
			ed2_1_id = ed2_1.getId();
			ed2_2_id = ed2_2.getId();
		} );
	}

	@Test
	public void testOwnerHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id,
					"owner"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id,
					"owner"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id,
					"owner"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id,
					"owner"
			);
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testOwnerSecEntityHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id,
					"owner"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id,
					"owner"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id,
					"owner"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id,
					"owner"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
		} );
	}

	@Test
	public void testReferences1HasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
					"references1"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
					"references1"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
					"references1"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
					"references1"
			);
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testReferences2HasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
					"references2"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
					"references2"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
					"references2"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged(
					auditReader,
					DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
					"references2"
			);
			assertEquals( 0, list.size() );
		} );
	}
}
