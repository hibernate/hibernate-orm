/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.dynamic;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SanityCheckTest extends BaseEnversFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicComponents/mapSanityCheck.hbm.xml" };
	}

	@Test
	@Priority(10)
	public void shouldInit() {
		Session session = getSession();
		session.getTransaction().begin();

		ManyToOneEntity manyToOne = getManyToOneEntity();
		ManyToManyEntity manyToMany = getManyToManyEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();

		PlainEntity plainEntity = getPlainEntity( manyToOne, manyToMany, oneToOne );

		session.save( manyToMany );
		session.save( manyToOne );
		session.save( oneToOne );
		session.save( plainEntity );

		session.getTransaction().commit();
		session.getTransaction().begin();
		PlainEntity load = (PlainEntity) session.load( PlainEntity.class, 1L );

		Assert.assertEquals( plainEntity, load );
		session.getTransaction().commit();

	}

	private PlainEntity getPlainEntity(ManyToOneEntity manyToOne, ManyToManyEntity manyToMany, OneToOneEntity oneToOne) {
		PlainComponent plainComponent = new PlainComponent();
		List<ManyToManyEntity> manyToManyEntityList = new ArrayList<ManyToManyEntity>();
		manyToManyEntityList.add( manyToMany );
		plainComponent.setManyToManyList( manyToManyEntityList );
		plainComponent.setComponentNote( "Note" );
		plainComponent.setOneToOneEntity( oneToOne );
		plainComponent.setManyToOneEntity( manyToOne );
		plainComponent.setInternalComponent( new InternalComponent( "Some val" ) );
		ArrayList<InternalComponent> internalComponents = new ArrayList<InternalComponent>();
		internalComponents.add( new InternalComponent( "test" ) );
		plainComponent.setInternalComponents( internalComponents );

		PlainEntity plainEntity = new PlainEntity();
		plainEntity.setId( 1L );
		plainEntity.setNote( "Plain note" );
		plainEntity.setComponent( plainComponent );
		return plainEntity;
	}

	private ManyToOneEntity getManyToOneEntity() {
		return new ManyToOneEntity( 1L, "ManyToOne" );
	}

	private OneToOneEntity getOneToOneEntity() {
		return new OneToOneEntity( 1L, "OneToOne" );
	}

	private ManyToManyEntity getManyToManyEntity() {
		return new ManyToManyEntity( 1L, "ManyToMany" );
	}

	@Test
	public void shouldFindRevisionBySimpleProperty() {
		ManyToOneEntity manyToOne = getManyToOneEntity();
		ManyToManyEntity manyToMany = getManyToManyEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();

		PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );


		//given (and result of shouldInitData()

		//when
		List resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add( AuditEntity.property( "component_componentNote" ).eq( "Note" ) )
				.getResultList();

		Assert.assertEquals( entity, resultList.get( 0 ) );
	}

	@Test
	public void shouldFindByInternalComponentProperty() {
		ManyToOneEntity manyToOne = getManyToOneEntity();
		ManyToManyEntity manyToMany = getManyToManyEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();

		PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );


		//given (and result of shouldInitData()

		//when
		List resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add(
						AuditEntity.property( "component_internalComponent_property" )
								.eq( entity.getComponent().getInternalComponent().getProperty() )
				)
				.getResultList();

		Assert.assertEquals( entity, resultList.get( 0 ) );
	}

	@Test
	public void shouldFailWhenQueryOnManyToMany() {
		ManyToManyEntity manyToMany = getManyToManyEntity();

		//when

		List<ManyToManyEntity> manyToManyEntities = new ArrayList<ManyToManyEntity>();
		manyToManyEntities.add( manyToMany );
		try {
			getAuditReader().createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add( AuditEntity.property( "component_manyToManyList" ).eq( manyToManyEntities ) )
					.getResultList();
			//then
			fail( "This should have generated an AuditException" );
		}
		catch ( Exception e ) {
			assertTyping( AuditException.class, e );
			assertEquals(
					"This type of relation (org.hibernate.envers.test.integration.components.dynamic.PlainEntity.component_manyToManyList) isn't supported and can't be used in queries.",
					e.getMessage()
			);
		}
	}

	@Test
	public void shouldFailWhenQueryOnManyToOne() {
		//when
		PlainEntity plainEntity = (PlainEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add( AuditEntity.relatedId( "component_manyToOneEntity" ).eq( getManyToOneEntity().getId() ) )
				.getResultList().get( 0 );

		//then
		Assert.assertEquals( getManyToOneEntity(), plainEntity.getComponent().getManyToOneEntity() );
	}

	@Test
	public void shouldFailWhenQueryOnOneToOne() {
		//when
		try {
			getAuditReader().createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add( AuditEntity.relatedId( "component_oneToOneEntity" ).eq( getOneToOneEntity().getId() ) )
					.getResultList();

			//then
			fail( "This should have generated an IllegalArgumentException" );
		}
		catch ( Exception e ) {
			assertTyping( IllegalArgumentException.class, e );
		}
	}
}
