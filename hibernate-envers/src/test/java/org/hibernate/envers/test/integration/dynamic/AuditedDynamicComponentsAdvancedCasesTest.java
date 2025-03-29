/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.dynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.components.dynamic.AdvancedEntity;
import org.hibernate.orm.test.envers.integration.components.dynamic.Age;
import org.hibernate.orm.test.envers.integration.components.dynamic.InternalComponent;
import org.hibernate.orm.test.envers.integration.components.dynamic.ManyToManyEntity;
import org.hibernate.orm.test.envers.integration.components.dynamic.ManyToOneEntity;
import org.hibernate.orm.test.envers.integration.components.dynamic.OneToOneEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import junit.framework.Assert;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * @author Lukasz Zuchowski (author at zuchos dot com)
 *         More advanced tests for dynamic component.
 */
@JiraKey(value = "HHH-8049")
public class AuditedDynamicComponentsAdvancedCasesTest extends BaseEnversFunctionalTestCase {

	public static final String PROP_BOOLEAN = "propBoolean";
	public static final String PROP_INT = "propInt";
	public static final String PROP_FLOAT = "propFloat";
	public static final String PROP_MANY_TO_ONE = "propManyToOne";
	public static final String PROP_ONE_TO_ONE = "propOneToOne";
	public static final String INTERNAL_COMPONENT = "internalComponent";
	public static final String INTERNAL_LIST = "internalList";
	public static final String INTERNAL_MAP = "internalMap";
	public static final String INTERNAL_MAP_WITH_MANY_TO_MANY = "internalMapWithEntities";
	public static final String INTERNAL_SET = "internalSet";
	public static final String INTERNAL_SET_OF_COMPONENTS = "internalSetOfComponents";
	public static final String AGE_USER_TYPE = "ageUserType";
	public static final String INTERNAL_LIST_OF_USER_TYPES = "internalListOfUserTypes";

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "false" );
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicComponents/mapAdvanced.hbm.xml" };
	}

	private OneToOneEntity getOneToOneEntity() {
		return new OneToOneEntity( 1L, "OneToOne" );
	}

	private ManyToManyEntity getManyToManyEntity() {
		return new ManyToManyEntity( 1L, "ManyToMany" );
	}

	private ManyToOneEntity getManyToOneEntity() {
		return new ManyToOneEntity( 1L, "ManyToOne" );
	}


	private AdvancedEntity getAdvancedEntity(ManyToOneEntity manyToOne, OneToOneEntity oneToOne, ManyToManyEntity manyToManyEntity) {
		AdvancedEntity advancedEntity = new AdvancedEntity();
		advancedEntity.setId( 1L );
		advancedEntity.setNote( "Test note" );
		advancedEntity.getDynamicConfiguration().put( PROP_BOOLEAN, true );
		advancedEntity.getDynamicConfiguration().put( PROP_INT, 19 );
		advancedEntity.getDynamicConfiguration().put( PROP_FLOAT, 15.9f );
		advancedEntity.getDynamicConfiguration().put( PROP_MANY_TO_ONE, manyToOne );
		advancedEntity.getDynamicConfiguration().put( PROP_ONE_TO_ONE, oneToOne );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_COMPONENT, new InternalComponent( "Internal value" ) );
		List<String> list = new ArrayList<String>();
		list.add( "One" );
		list.add( "Two" );
		list.add( "Three" );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_LIST, list );
		Map<String, String> map = new HashMap<String, String>();
		map.put( "one", "1" );
		map.put( "two", "2" );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_MAP, map );
		Map<String, ManyToManyEntity> mapWithManyToMany = new HashMap<String, ManyToManyEntity>();
		mapWithManyToMany.put( "entity1", manyToManyEntity );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_MAP_WITH_MANY_TO_MANY, mapWithManyToMany );
		Set<String> set = new HashSet<String>();
		set.add( "Une" );
		set.add( "Due" );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_SET, set );
		Set<InternalComponent> componentSet = new HashSet<InternalComponent>();
		componentSet.add( new InternalComponent( "Ein" ) );
		componentSet.add( new InternalComponent( "Zwei" ) );
		advancedEntity.getDynamicConfiguration().put( INTERNAL_SET_OF_COMPONENTS, componentSet );
		advancedEntity.getDynamicConfiguration().put( AGE_USER_TYPE, new Age( 18 ) );
		List<Age> ages = new ArrayList<Age>();
		ages.add( new Age( 1 ) );
		ages.add( new Age( 2 ) );
		ages.add( new Age( 3 ) );

		advancedEntity.getDynamicConfiguration().put( INTERNAL_LIST_OF_USER_TYPES, ages );
		return advancedEntity;
	}

	@Test
	@Priority(10)
	//smoke test to make sure that hibernate & envers are working with the entity&mappings
	public void shouldInitData() {
		//given
		ManyToOneEntity manyToOne = getManyToOneEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();
		ManyToManyEntity manyToManyEntity = getManyToManyEntity();
		AdvancedEntity advancedEntity = getAdvancedEntity( manyToOne, oneToOne, manyToManyEntity );

		//rev 1
		Session session = openSession();
		session.getTransaction().begin();
		session.persist( manyToOne );
		session.persist( oneToOne );
		session.persist( manyToManyEntity );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 2
		session.getTransaction().begin();
		InternalComponent internalComponent = (InternalComponent) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_COMPONENT );
		internalComponent.setProperty( "new value" );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 3
		session.getTransaction().begin();
		List<String> internalList = (List) advancedEntity.getDynamicConfiguration().get( INTERNAL_LIST );
		internalList.add( "four" );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 4
		session.getTransaction().begin();
		Map<String, String> map = (Map) advancedEntity.getDynamicConfiguration().get( INTERNAL_MAP );
		map.put( "three", "3" );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 5
		session.getTransaction().begin();
		Map<String, ManyToManyEntity> mapWithManyToMany = (Map) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_MAP_WITH_MANY_TO_MANY );
		ManyToManyEntity manyToManyEntity2 = new ManyToManyEntity( 2L, "new value" );
		mapWithManyToMany.put( "entity2", manyToManyEntity2 );
		session.persist( manyToManyEntity2 );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 6
		session.getTransaction().begin();
		mapWithManyToMany = (Map) advancedEntity.getDynamicConfiguration().get( INTERNAL_MAP_WITH_MANY_TO_MANY );
		mapWithManyToMany.clear();
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 7
		session.getTransaction().begin();
		Set<InternalComponent> internalComponentSet = (Set) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_SET_OF_COMPONENTS );
		internalComponentSet.add( new InternalComponent( "drei" ) );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 8
		session.getTransaction().begin();
		advancedEntity.getDynamicConfiguration().put( AGE_USER_TYPE, new Age( 19 ) );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev 9
		session.getTransaction().begin();
		List<Age> ages = (List<Age>) advancedEntity.getDynamicConfiguration().get( INTERNAL_LIST_OF_USER_TYPES );
		ages.add( new Age( 4 ) );
		session.persist( advancedEntity );
		session.getTransaction().commit();

		//rev this, should not create revision
		session.getTransaction().begin();
		session.getTransaction().commit();

		//sanity check. Loaded entity should be equal to one that we created.
		AdvancedEntity advancedEntityActual = (AdvancedEntity) session.getReference( AdvancedEntity.class, 1L );

		Assert.assertEquals( advancedEntity, advancedEntityActual );
	}


	@Test
	public void shouldMakeRevisions() {
		Session session = openSession();
		session.getTransaction().begin();
		//given & when shouldInitData
		ManyToOneEntity manyToOne = getManyToOneEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();
		ManyToManyEntity manyToManyEntity = getManyToManyEntity();
		AdvancedEntity advancedEntity = getAdvancedEntity( manyToOne, oneToOne, manyToManyEntity );

		//then v1
		AdvancedEntity ver1 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				1
		);
		Assert.assertEquals( advancedEntity, ver1 );

		//then v2
		InternalComponent internalComponent = (InternalComponent) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_COMPONENT );
		internalComponent.setProperty( "new value" );

		AdvancedEntity ver2 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				2
		);
		Assert.assertEquals( advancedEntity, ver2 );

		//then v3

		List internalList = (List) advancedEntity.getDynamicConfiguration().get( INTERNAL_LIST );
		internalList.add( "four" );

		AdvancedEntity ver3 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				3
		);
		Assert.assertEquals( advancedEntity, ver3 );

		//then v4
		Map<String, String> map = (Map) advancedEntity.getDynamicConfiguration().get( INTERNAL_MAP );
		map.put( "three", "3" );

		AdvancedEntity ver4 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				4
		);
		Assert.assertEquals( advancedEntity, ver4 );

		//then v5
		Map<String, ManyToManyEntity> mapWithManyToMany = (Map) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_MAP_WITH_MANY_TO_MANY );
		ManyToManyEntity manyToManyEntity2 = new ManyToManyEntity( 2L, "new value" );
		mapWithManyToMany.put( "entity2", manyToManyEntity2 );

		AdvancedEntity ver5 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				5
		);
		Assert.assertEquals( advancedEntity, ver5 );

		//then v6
		mapWithManyToMany = (Map) advancedEntity.getDynamicConfiguration().get( INTERNAL_MAP_WITH_MANY_TO_MANY );
		mapWithManyToMany.clear();

		AdvancedEntity ver6 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				6
		);
		Assert.assertEquals( advancedEntity, ver6 );

		//then v7
		Set<InternalComponent> internalComponentSet = (Set) advancedEntity.getDynamicConfiguration()
				.get( INTERNAL_SET_OF_COMPONENTS );
		internalComponentSet.add( new InternalComponent( "drei" ) );

		AdvancedEntity ver7 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				7
		);
		Assert.assertEquals( advancedEntity, ver7 );

		//then v8
		advancedEntity.getDynamicConfiguration().put( AGE_USER_TYPE, new Age( 19 ) );


		AdvancedEntity ver8 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				8
		);
		Assert.assertEquals( advancedEntity, ver8 );

		//then v9
		List<Age> ages = (List<Age>) advancedEntity.getDynamicConfiguration().get( INTERNAL_LIST_OF_USER_TYPES );
		ages.add( new Age( 4 ) );

		AdvancedEntity ver9 = getAuditReader().find(
				AdvancedEntity.class,
				advancedEntity.getId(),
				9
		);
		Assert.assertEquals( advancedEntity, ver9 );

		session.getTransaction().commit();
	}

	@Test
	public void testOfQueryOnDynamicComponent() {
		//given (and result of shouldInitData()
		AdvancedEntity entity = getAdvancedEntity( getManyToOneEntity(), getOneToOneEntity(), getManyToManyEntity() );

		//when
		ManyToOneEntity manyToOneEntity = (ManyToOneEntity) entity.getDynamicConfiguration().get( PROP_MANY_TO_ONE );
		List resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AdvancedEntity.class, 1 )
				.add(
						AuditEntity.relatedId( "dynamicConfiguration_" + PROP_MANY_TO_ONE )
								.eq( manyToOneEntity.getId() )
				)
				.getResultList();

		//then
		Assert.assertEquals( entity, resultList.get( 0 ) );

		//when
		InternalComponent internalComponent = (InternalComponent) entity.getDynamicConfiguration().get( INTERNAL_COMPONENT );
		resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AdvancedEntity.class, 1 )
				.add(
						AuditEntity.property( "dynamicConfiguration_" + INTERNAL_COMPONENT+"_property")
								.eq( internalComponent.getProperty() )
				)
				.getResultList();

		//then
		Assert.assertEquals( entity, resultList.get( 0 ) );

		//when
		try {
			OneToOneEntity oneToOneEntity = (OneToOneEntity) entity.getDynamicConfiguration().get( PROP_ONE_TO_ONE );
			getAuditReader().createQuery()
					.forEntitiesAtRevision( AdvancedEntity.class, 1 )
					.add( AuditEntity.property( "dynamicConfiguration_" + PROP_ONE_TO_ONE ).eq( oneToOneEntity ) )
					.getResultList();

			//then
			Assert.fail();
		}
		catch ( Exception e ) {
			if ( getSession().getTransaction().isActive() ) {
				getSession().getTransaction().rollback();
			}

			assertTyping( IllegalArgumentException.class, e );
		}
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9 ),
				getAuditReader().getRevisions( AdvancedEntity.class, 1L )
		);
	}
}
