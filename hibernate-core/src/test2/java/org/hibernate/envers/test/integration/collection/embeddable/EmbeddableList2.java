/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestNoProxyEntity;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity2;
import org.hibernate.envers.test.entities.components.relations.ManyToOneEagerComponent;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks if many-to-one relations inside an embedded component list are being audited.
 *
 * @author thiagolrc
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableList2 extends BaseEnversJPAFunctionalTestCase {
	private Integer ele_id1 = null;

	private StrTestNoProxyEntity entity1 = new StrTestNoProxyEntity( "strTestEntity1" );
	private StrTestNoProxyEntity entity2 = new StrTestNoProxyEntity( "strTestEntity2" );
	private StrTestNoProxyEntity entity3 = new StrTestNoProxyEntity( "strTestEntity3" );
	private StrTestNoProxyEntity entity4 = new StrTestNoProxyEntity( "strTestEntity3" );
	private StrTestNoProxyEntity entity4Copy = null;

	private ManyToOneEagerComponent manyToOneComponent1 = new ManyToOneEagerComponent( entity1, "dataComponent1" );
	private ManyToOneEagerComponent manyToOneComponent2 = new ManyToOneEagerComponent( entity2, "dataComponent2" );
	private ManyToOneEagerComponent manyToOneComponent4 = new ManyToOneEagerComponent( entity4, "dataComponent4" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EmbeddableListEntity2.class, StrTestNoProxyEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 (ele1: saving a list with 1 many-to-one component)
		em.getTransaction().begin();
		EmbeddableListEntity2 ele1 = new EmbeddableListEntity2();
		em.persist( entity1 ); //persisting the entities referenced by the components
		em.persist( entity2 );
		ele1.getComponentList().add( manyToOneComponent1 );
		em.persist( ele1 );
		em.getTransaction().commit();
		ele_id1 = ele1.getId();

		// Revision 2 (ele1: changing the component)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		ele1.getComponentList().clear();
		ele1.getComponentList().add( manyToOneComponent2 );
		em.getTransaction().commit();

		//Revision 3 (ele1: putting back the many-to-one component to the list)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		ele1.getComponentList().add( manyToOneComponent1 );
		em.getTransaction().commit();

		// Revision 4 (ele1: changing the component's entity)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		em.persist( entity3 );
		ele1.getComponentList().get( ele1.getComponentList().indexOf( manyToOneComponent2 ) ).setEntity( entity3 );
		ele1.getComponentList()
				.get( ele1.getComponentList().indexOf( manyToOneComponent2 ) )
				.setData( "dataComponent3" );
		em.getTransaction().commit();

		// Revision 5 (ele1: adding a new many-to-one component)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		em.persist( entity4 );
		entity4Copy = new StrTestNoProxyEntity( entity4.getStr(), entity4.getId() );
		ele1.getComponentList().add( manyToOneComponent4 );
		em.getTransaction().commit();

		// Revision 6 (ele1: changing the component's entity properties)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		ele1.getComponentList()
				.get( ele1.getComponentList().indexOf( manyToOneComponent4 ) )
				.getEntity()
				.setStr( "sat4" );
		em.getTransaction().commit();

		// Revision 7 (ele1: removing component)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		ele1.getComponentList().remove( ele1.getComponentList().indexOf( manyToOneComponent4 ) );
		em.getTransaction().commit();

		// Revision 8 (ele1: removing all)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity2.class, ele1.getId() );
		em.remove( ele1 );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2, 3, 4, 5, 7, 8 ),
				getAuditReader().getRevisions( EmbeddableListEntity2.class, ele_id1 )
		);
		assertEquals(
				Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity1.getId() )
		);
		assertEquals(
				Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity2.getId() )
		);
		assertEquals(
				Arrays.asList( 4 ), getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity3.getId() )
		);
		assertEquals(
				Arrays.asList( 5, 6 ),
				getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity4.getId() )
		);
	}

	@Test
	public void testManyToOneComponentList() {
		// Revision 1: many-to-one component1 in the list
		EmbeddableListEntity2 rev1 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 1 );
		assertNotNull( "Revision not found", rev1 );
		assertTrue( "The component collection was not audited", rev1.getComponentList().size() > 0 );
		assertEquals(
				"The component primitive property was not audited",
				"dataComponent1", rev1.getComponentList().get( 0 ).getData()
		);
		assertEquals(
				"The component manyToOne reference was not audited",
				entity1, rev1.getComponentList().get( 0 ).getEntity()
		);
	}

	@Test
	public void testHistoryOfEle1() {
		// Revision 1: many-to-one component in the list
		assertEquals(
				Arrays.asList( new ManyToOneEagerComponent( entity1, "dataComponent1" ) ),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 1 ).getComponentList()
		);

		// Revision 2: many-to-one component in the list
		assertEquals(
				Arrays.asList( new ManyToOneEagerComponent( entity2, "dataComponent2" ) ),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 2 ).getComponentList()
		);

		// Revision 3: two many-to-one components in the list
		assertEquals(
				Arrays.asList(
						new ManyToOneEagerComponent( entity2, "dataComponent2" ),
						new ManyToOneEagerComponent( entity1, "dataComponent1" )
				),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 3 ).getComponentList()
		);

		// Revision 4: second component edited and first one in the list
		assertEquals(
				Arrays.asList(
						new ManyToOneEagerComponent( entity3, "dataComponent3" ),
						new ManyToOneEagerComponent( entity1, "dataComponent1" )
				),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 4 ).getComponentList()
		);

		// Revision 5: fourth component added in the list
		assertEquals(
				Arrays.asList(
						new ManyToOneEagerComponent( entity3, "dataComponent3" ),
						new ManyToOneEagerComponent( entity1, "dataComponent1" ),
						new ManyToOneEagerComponent( entity4Copy, "dataComponent4" )
				),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 5 ).getComponentList()
		);

		// Revision 6: changing fourth component property
		assertEquals(
				Arrays.asList(
						new ManyToOneEagerComponent( entity3, "dataComponent3" ),
						new ManyToOneEagerComponent( entity1, "dataComponent1" ),
						new ManyToOneEagerComponent( entity4, "dataComponent4" )
				),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 6 ).getComponentList()
		);

		// Revision 7: removing component number four
		assertEquals(
				Arrays.asList(
						new ManyToOneEagerComponent( entity3, "dataComponent3" ),
						new ManyToOneEagerComponent( entity1, "dataComponent1" )
				),
				getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 7 ).getComponentList()
		);

		assertNull( getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 8 ) );
	}
}