/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestNoProxyEntity;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity2;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneEagerComponent;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks if many-to-one relations inside an embedded component list are being audited.
 *
 * @author thiagolrc
 */
@JiraKey(value = "HHH-6613")
@EnversTest
@Jpa(annotatedClasses = {EmbeddableListEntity2.class, StrTestNoProxyEntity.class})
public class EmbeddableList2 {
	private Integer ele_id1 = null;

	private StrTestNoProxyEntity entity1;
	private StrTestNoProxyEntity entity2;
	private StrTestNoProxyEntity entity3;
	private StrTestNoProxyEntity entity4;
	private StrTestNoProxyEntity entity4Copy;

	private ManyToOneEagerComponent manyToOneComponent1;
	private ManyToOneEagerComponent manyToOneComponent2;
	private ManyToOneEagerComponent manyToOneComponent4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (ele1: saving a list with 1 many-to-one component)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			EmbeddableListEntity2 ele1 = new EmbeddableListEntity2();
			entity1 = new StrTestNoProxyEntity( "strTestEntity1" );
			em.persist( entity1 ); //persisting the entities referenced by the components
			entity2 = new StrTestNoProxyEntity( "strTestEntity2" );
			em.persist( entity2 );
			manyToOneComponent1 = new ManyToOneEagerComponent( entity1, "dataComponent1" );
			ele1.getComponentList().add( manyToOneComponent1 );
			em.persist( ele1 );
			ele_id1 = ele1.getId();
			em.getTransaction().commit();

			// Revision 2 (ele1: changing the component)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			ele1.getComponentList().clear();
			manyToOneComponent2 = new ManyToOneEagerComponent( entity2, "dataComponent2" );
			ele1.getComponentList().add( manyToOneComponent2 );
			em.getTransaction().commit();

			//Revision 3 (ele1: putting back the many-to-one component to the list)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			ele1.getComponentList().add( manyToOneComponent1 );
			em.getTransaction().commit();

			// Revision 4 (ele1: changing the component's entity)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			entity3 = new StrTestNoProxyEntity( "strTestEntity3" );
			em.persist( entity3 );
			ele1.getComponentList().get( ele1.getComponentList().indexOf( manyToOneComponent2 ) ).setEntity( entity3 );
			ele1.getComponentList()
					.get( ele1.getComponentList().indexOf( manyToOneComponent2 ) )
					.setData( "dataComponent3" );
			em.getTransaction().commit();

			// Revision 5 (ele1: adding a new many-to-one component)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			entity4 = new StrTestNoProxyEntity( "strTestEntity4" );
			em.persist( entity4 );
			entity4Copy = new StrTestNoProxyEntity( entity4.getStr(), entity4.getId() );
			manyToOneComponent4 = new ManyToOneEagerComponent( entity4, "dataComponent4" );
			ele1.getComponentList().add( manyToOneComponent4 );
			em.getTransaction().commit();

			// Revision 6 (ele1: changing the component's entity properties)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			ele1.getComponentList()
					.get( ele1.getComponentList().indexOf( manyToOneComponent4 ) )
					.getEntity()
					.setStr( "sat4" );
			em.getTransaction().commit();

			// Revision 7 (ele1: removing component)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			ele1.getComponentList().remove( ele1.getComponentList().indexOf( manyToOneComponent4 ) );
			em.getTransaction().commit();

			// Revision 8 (ele1: removing all)
			em.getTransaction().begin();
			ele1 = em.find( EmbeddableListEntity2.class, ele_id1 );
			em.remove( ele1 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3, 4, 5, 7, 8 ),
					auditReader.getRevisions( EmbeddableListEntity2.class, ele_id1 )
			);
			assertEquals(
					Arrays.asList( 1 ), auditReader.getRevisions( StrTestNoProxyEntity.class, entity1.getId() )
			);
			assertEquals(
					Arrays.asList( 1 ), auditReader.getRevisions( StrTestNoProxyEntity.class, entity2.getId() )
			);
			assertEquals(
					Arrays.asList( 4 ), auditReader.getRevisions( StrTestNoProxyEntity.class, entity3.getId() )
			);
			assertEquals(
					Arrays.asList( 5, 6 ),
					auditReader.getRevisions( StrTestNoProxyEntity.class, entity4.getId() )
			);
		} );
	}

	@Test
	public void testManyToOneComponentList(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// Revision 1: many-to-one component1 in the list
			EmbeddableListEntity2 rev1 = auditReader.find( EmbeddableListEntity2.class, ele_id1, 1 );
			assertNotNull( rev1, "Revision not found" );
			assertTrue( rev1.getComponentList().size() > 0, "The component collection was not audited" );
			assertEquals(
					"dataComponent1", rev1.getComponentList().get( 0 ).getData(),
					"The component primitive property was not audited"
			);
			assertEquals(
					entity1, rev1.getComponentList().get( 0 ).getEntity(),
					"The component manyToOne reference was not audited"
			);
		} );
	}

	@Test
	public void testHistoryOfEle1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// Revision 1: many-to-one component in the list
			assertEquals(
					Arrays.asList( new ManyToOneEagerComponent( entity1, "dataComponent1" ) ),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 1 ).getComponentList()
			);

			// Revision 2: many-to-one component in the list
			assertEquals(
					Arrays.asList( new ManyToOneEagerComponent( entity2, "dataComponent2" ) ),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 2 ).getComponentList()
			);

			// Revision 3: two many-to-one components in the list
			assertEquals(
					Arrays.asList(
							new ManyToOneEagerComponent( entity2, "dataComponent2" ),
							new ManyToOneEagerComponent( entity1, "dataComponent1" )
					),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 3 ).getComponentList()
			);

			// Revision 4: second component edited and first one in the list
			assertEquals(
					Arrays.asList(
							new ManyToOneEagerComponent( entity3, "dataComponent3" ),
							new ManyToOneEagerComponent( entity1, "dataComponent1" )
					),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 4 ).getComponentList()
			);

			// Revision 5: fourth component added in the list
			assertEquals(
					Arrays.asList(
							new ManyToOneEagerComponent( entity3, "dataComponent3" ),
							new ManyToOneEagerComponent( entity1, "dataComponent1" ),
							new ManyToOneEagerComponent( entity4Copy, "dataComponent4" )
					),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 5 ).getComponentList()
			);

			// Revision 6: changing fourth component property
			assertEquals(
					Arrays.asList(
							new ManyToOneEagerComponent( entity3, "dataComponent3" ),
							new ManyToOneEagerComponent( entity1, "dataComponent1" ),
							new ManyToOneEagerComponent( entity4, "dataComponent4" )
					),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 6 ).getComponentList()
			);

			// Revision 7: removing component number four
			assertEquals(
					Arrays.asList(
							new ManyToOneEagerComponent( entity3, "dataComponent3" ),
							new ManyToOneEagerComponent( entity1, "dataComponent1" )
					),
					auditReader.find( EmbeddableListEntity2.class, ele_id1, 7 ).getComponentList()
			);

			assertNull( auditReader.find( EmbeddableListEntity2.class, ele_id1, 8 ) );
		} );
	}
}
