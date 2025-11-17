/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@EnversTest
@Jpa(
		xmlMappings = "mappings/dynamicComponents/mapSanityCheck.hbm.xml",
		annotatedClasses = {
				PlainEntity.class,
				ManyToOneEntity.class,
				ManyToManyEntity.class,
				OneToOneEntity.class
		}
)
public class SanityCheckTest {

	@BeforeClassTemplate
	public void shouldInit(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			ManyToOneEntity manyToOne = getManyToOneEntity();
			ManyToManyEntity manyToMany = getManyToManyEntity();
			OneToOneEntity oneToOne = getOneToOneEntity();

			PlainEntity plainEntity = getPlainEntity( manyToOne, manyToMany, oneToOne );

			em.persist( manyToMany );
			em.persist( manyToOne );
			em.persist( oneToOne );
			em.persist( plainEntity );
		} );

		scope.inTransaction( em -> {
			PlainEntity load = em.getReference( PlainEntity.class, 1L );
			PlainEntity expected = getPlainEntity( getManyToOneEntity(), getManyToManyEntity(), getOneToOneEntity() );
			assertEquals( expected, load );
		} );
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
	public void shouldFindRevisionBySimpleProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ManyToOneEntity manyToOne = getManyToOneEntity();
			ManyToManyEntity manyToMany = getManyToManyEntity();
			OneToOneEntity oneToOne = getOneToOneEntity();

			PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );

			//when
			List resultList = auditReader.createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add( AuditEntity.property( "component_componentNote" ).eq( "Note" ) )
					.getResultList();

			assertEquals( entity, resultList.get( 0 ) );
		} );
	}

	@Test
	public void shouldFindByInternalComponentProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ManyToOneEntity manyToOne = getManyToOneEntity();
			ManyToManyEntity manyToMany = getManyToManyEntity();
			OneToOneEntity oneToOne = getOneToOneEntity();

			PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );

			//when
			List resultList = auditReader.createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add(
							AuditEntity.property( "component_internalComponent_property" )
									.eq( entity.getComponent().getInternalComponent().getProperty() )
					)
					.getResultList();

			assertEquals( entity, resultList.get( 0 ) );
		} );
	}

	@Test
	public void shouldFailWhenQueryOnManyToMany(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ManyToManyEntity manyToMany = getManyToManyEntity();

			//when
			List<ManyToManyEntity> manyToManyEntities = new ArrayList<ManyToManyEntity>();
			manyToManyEntities.add( manyToMany );
			try {
				auditReader.createQuery()
						.forEntitiesAtRevision( PlainEntity.class, 1 )
						.add( AuditEntity.property( "component_manyToManyList" ).eq( manyToManyEntities ) )
						.getResultList();
				//then
				fail( "This should have generated an AuditException" );
			}
			catch ( Exception e ) {
				assertTyping( AuditException.class, e );
				assertEquals(
						"This type of relation (org.hibernate.orm.test.envers.integration.components.dynamic.PlainEntity." +
								"component_manyToManyList) can't be used in audit query restrictions.",
						e.getMessage()
				);
			}
		} );
	}

	@Test
	public void shouldFailWhenQueryOnManyToOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			//when
			PlainEntity plainEntity = (PlainEntity) auditReader.createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add( AuditEntity.relatedId( "component_manyToOneEntity" ).eq( getManyToOneEntity().getId() ) )
					.getResultList().get( 0 );

			//then
			assertEquals( getManyToOneEntity(), plainEntity.getComponent().getManyToOneEntity() );
		} );
	}

	@Test
	public void shouldFailWhenQueryOnOneToOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			//when
			try {
				auditReader.createQuery()
						.forEntitiesAtRevision( PlainEntity.class, 1 )
						.add( AuditEntity.relatedId( "component_oneToOneEntity" ).eq( getOneToOneEntity().getId() ) )
						.getResultList();

				//then
				fail( "This should have generated an IllegalArgumentException" );
			}
			catch ( Exception e ) {
				assertTyping( IllegalArgumentException.class, e );
			}
		} );
	}
}
