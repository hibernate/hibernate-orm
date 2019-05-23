/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestNoProxyEntity;
import org.hibernate.envers.test.support.domains.collections.EmbeddableListEntity2;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneEagerComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Checks if many-to-one relations inside an embedded component list are being audited.
 *
 * @author thiagolrc
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableList2Test extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ele_id1 = null;

	private StrTestNoProxyEntity entity1 = new StrTestNoProxyEntity( "strTestEntity1" );
	private StrTestNoProxyEntity entity2 = new StrTestNoProxyEntity( "strTestEntity2" );
	private StrTestNoProxyEntity entity3 = new StrTestNoProxyEntity( "strTestEntity3" );
	private StrTestNoProxyEntity entity4 = new StrTestNoProxyEntity( "strTestEntity3" );
	private StrTestNoProxyEntity entity4Copy = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableListEntity2.class, StrTestNoProxyEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final ManyToOneEagerComponent manyToOneComponent1 = new ManyToOneEagerComponent( entity1, "dataComponent1" );
		final ManyToOneEagerComponent manyToOneComponent2 = new ManyToOneEagerComponent( entity2, "dataComponent2" );
		final ManyToOneEagerComponent manyToOneComponent4 = new ManyToOneEagerComponent( entity4, "dataComponent4" );

		inTransactions(
				// Revision 1
				entityManager -> {
					final EmbeddableListEntity2 ele1 = new EmbeddableListEntity2();
					entityManager.persist( entity1 ); //persisting the entities referenced by the components
					entityManager.persist( entity2 );
					ele1.getComponentList().add( manyToOneComponent1 );
					entityManager.persist( ele1 );

					ele_id1 = ele1.getId();
				},

				// Revision 2
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					ele1.getComponentList().clear();
					ele1.getComponentList().add( manyToOneComponent2 );
				},

				// Revision 3
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					ele1.getComponentList().add( manyToOneComponent1 );
				},

				// Revision 4
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					entityManager.persist( entity3 );
					ele1.getComponentList()
							.get( ele1.getComponentList().indexOf( manyToOneComponent2 ) )
							.setEntity( entity3 );
					ele1.getComponentList()
							.get( ele1.getComponentList().indexOf( manyToOneComponent2 ) )
							.setData( "dataComponent3" );
				},

				// Revision 5
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					entityManager.persist( entity4 );
					entity4Copy = new StrTestNoProxyEntity( entity4.getStr(), entity4.getId() );
					ele1.getComponentList().add( manyToOneComponent4 );
				},

				// Revision 6
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					ele1.getComponentList()
							.get( ele1.getComponentList().indexOf( manyToOneComponent4 ) )
							.getEntity()
							.setStr( "sat4" );
				},

				// Revision 7
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					ele1.getComponentList().remove( ele1.getComponentList().indexOf( manyToOneComponent4 ) );
				},

				// Revision 8
				entityManager -> {
					final EmbeddableListEntity2 ele1 = entityManager.find( EmbeddableListEntity2.class, ele_id1 );
					entityManager.remove( ele1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmbeddableListEntity2.class, ele_id1 ), contains( 1, 2, 3, 4, 5, 7, 8 ) );

		assertThat( getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity1.getId() ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity2.getId() ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity3.getId() ), contains( 4 ) );
		assertThat( getAuditReader().getRevisions( StrTestNoProxyEntity.class, entity4.getId() ), contains( 5, 6 ) );
	}

	@DynamicTest
	public void testManyToOneComponentList() {
		// Revision 1: many-to-one component1 in the list
		final EmbeddableListEntity2 rev1 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 1 );
		assertThat( rev1, notNullValue() );
		assertThat( rev1.getComponentList(), CollectionMatchers.isNotEmpty() );
		assertThat( rev1.getComponentList().get( 0 ).getData(), equalTo( "dataComponent1" ) );
		assertThat( rev1.getComponentList().get( 0 ).getEntity(), equalTo( entity1 ) );
	}

	@DynamicTest
	public void testHistoryOfEle1() {
		final ManyToOneEagerComponent comp1 = new ManyToOneEagerComponent( entity1, "dataComponent1" );
		final ManyToOneEagerComponent comp2 = new ManyToOneEagerComponent( entity2, "dataComponent2" );
		final ManyToOneEagerComponent comp3 = new ManyToOneEagerComponent( entity3, "dataComponent3" );
		final ManyToOneEagerComponent comp4 = new ManyToOneEagerComponent( entity4, "dataComponent4" );
		final ManyToOneEagerComponent comp4Copy = new ManyToOneEagerComponent( entity4Copy, "dataComponent4" );

		// Revision 1: many-to-one component in the list
		final EmbeddableListEntity2 rev1 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 1 );
		assertThat( rev1.getComponentList(), contains( comp1 ) );

		// Revision 2: many-to-one component in the list
		final EmbeddableListEntity2 rev2 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 2 );
		assertThat( rev2.getComponentList(), contains( comp2 ) );

		// Revision 3: two many-to-one components in the list
		final EmbeddableListEntity2 rev3 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 3 );
		assertThat( rev3.getComponentList(), contains( comp2, comp1 ) );

		// Revision 4: second component edited and first one in the list
		final EmbeddableListEntity2 rev4 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 4 );
		assertThat( rev4.getComponentList(), contains( comp3, comp1 ) );

		// Revision 5: fourth component added in the list
		final EmbeddableListEntity2 rev5 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 5 );
		assertThat( rev5.getComponentList(), contains( comp3, comp1, comp4Copy ) );

		// Revision 6: changing fourth component property
		final EmbeddableListEntity2 rev6 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 6 );
		assertThat( rev6.getComponentList(), contains( comp3, comp1, comp4 ) );

		// Revision 7: removing component number four
		final EmbeddableListEntity2 rev7 = getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 7 );
		assertThat( rev7.getComponentList(), contains( comp3, comp1 ) );

		assertThat( getAuditReader().find( EmbeddableListEntity2.class, ele_id1, 8 ), nullValue() );
	}
}