/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefIngEntity;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {ListRefEdEntity.class, ListRefIngEntity.class})
public class HasChangedMergeTest extends AbstractModifiedFlagsEntityTest {
	private Integer parent1Id = null;
	private Integer child1Id = null;

	private Integer parent2Id = null;
	private Integer child2Id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - data preparation
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			ListRefEdEntity parent1 = new ListRefEdEntity( 1, "initial data" );
			parent1.setReffering( new ArrayList<>() ); // Empty collection is not the same as null reference.
			ListRefEdEntity parent2 = new ListRefEdEntity( 2, "initial data" );
			parent2.setReffering( new ArrayList<>() );
			em.persist( parent1 );
			em.persist( parent2 );
			parent1Id = parent1.getId();
			parent2Id = parent2.getId();
			em.getTransaction().commit();
		} );

		// Revision 2 - inserting new child entity and updating parent
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			ListRefEdEntity parent1 = em.find( ListRefEdEntity.class, parent1Id );
			ListRefIngEntity child1 = new ListRefIngEntity( 1, "initial data", parent1 );
			em.persist( child1 );
			parent1.setData( "updated data" );
			em.merge( parent1 );
			child1Id = child1.getId();
			em.getTransaction().commit();
		} );

		// Revision 3 - updating parent, flushing and adding new child
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			ListRefEdEntity parent2 = em.find( ListRefEdEntity.class, parent2Id );
			parent2.setData( "updated data" );
			em.merge( parent2 );
			em.flush();
			ListRefIngEntity child2 = new ListRefIngEntity( 2, "initial data", parent2 );
			em.persist( child2 );
			child2Id = child2.getId();
			em.getTransaction().commit();
		} );
	}

	@Test
	@JiraKey(value = "HHH-7948")
	public void testOneToManyInsertChildUpdateParent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ListRefEdEntity.class, parent1Id, "data" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ListRefEdEntity.class, parent1Id, "reffering" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ListRefIngEntity.class, child1Id, "reference" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7948")
	public void testOneToManyUpdateParentInsertChild(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ListRefEdEntity.class, parent2Id, "data" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ListRefEdEntity.class, parent2Id, "reffering" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ListRefIngEntity.class, child2Id, "reference" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
		} );
	}
}
