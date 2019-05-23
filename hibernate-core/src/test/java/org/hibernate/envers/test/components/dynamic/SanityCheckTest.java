/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.dynamic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.dynamic.InternalComponent;
import org.hibernate.envers.test.support.domains.components.dynamic.ManyToManyEntity;
import org.hibernate.envers.test.support.domains.components.dynamic.ManyToOneEntity;
import org.hibernate.envers.test.support.domains.components.dynamic.OneToOneEntity;
import org.hibernate.envers.test.support.domains.components.dynamic.PlainComponent;
import org.hibernate.envers.test.support.domains.components.dynamic.PlainEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

/**
 * @author Lukasz Zuchowski
 */
@Disabled("NYI - Fails because JavaTypeDescriptor not yet resolved")
public class SanityCheckTest extends EnversSessionFactoryBasedFunctionalTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "dynamic-components/MapSanityCheck.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final ManyToOneEntity manyToOne = getManyToOneEntity();
		final ManyToManyEntity manyToMany = getManyToManyEntity();
		final OneToOneEntity oneToOne = getOneToOneEntity();

		final PlainEntity plainEntity = getPlainEntity( manyToOne, manyToMany, oneToOne );

		inTransactions(
				session -> {
					session.save( manyToMany );
					session.save( manyToOne );
					session.save( oneToOne );
					session.save( plainEntity );
				},
				session -> {
					PlainEntity load = session.load( PlainEntity.class, plainEntity.getId() );
					assertThat( load, equalTo( plainEntity ) );
				}
		);
	}

	private PlainEntity getPlainEntity(ManyToOneEntity manyToOne, ManyToManyEntity manyToMany, OneToOneEntity oneToOne) {
		PlainComponent plainComponent = new PlainComponent();
		List<ManyToManyEntity> manyToManyEntityList = new ArrayList<>();
		manyToManyEntityList.add( manyToMany );
		plainComponent.setManyToManyList( manyToManyEntityList );
		plainComponent.setComponentNote( "Note" );
		plainComponent.setOneToOneEntity( oneToOne );
		plainComponent.setManyToOneEntity( manyToOne );
		plainComponent.setInternalComponent( new InternalComponent( "Some val" ) );
		ArrayList<InternalComponent> internalComponents = new ArrayList<>();
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

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void shouldFindRevisionBySimpleProperty() {
		ManyToOneEntity manyToOne = getManyToOneEntity();
		ManyToManyEntity manyToMany = getManyToManyEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();

		PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );

		List<PlainEntity> resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add( AuditEntity.property( "component_componentNote" ).eq( "Note" ) )
				.getResultList();

		assertThat( resultList.get( 0 ), equalTo( entity ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void shouldFindByInternalComponentProperty() {
		ManyToOneEntity manyToOne = getManyToOneEntity();
		ManyToManyEntity manyToMany = getManyToManyEntity();
		OneToOneEntity oneToOne = getOneToOneEntity();

		PlainEntity entity = getPlainEntity( manyToOne, manyToMany, oneToOne );

		List<PlainEntity> resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add(
						AuditEntity.property( "component_internalComponent_property" )
								.eq( entity.getComponent().getInternalComponent().getProperty() ) )
				.getResultList();

		assertThat( resultList.get( 0 ), equalTo( entity ) );
	}

	@DynamicTest
	public void shouldFailWhenQueryOnManyToMany() {
		final List<ManyToManyEntity> manyToManyEntities = new ArrayList<>();
		manyToManyEntities.add( getManyToManyEntity() );
		try {
			getAuditReader().createQuery()
					.forEntitiesAtRevision( PlainEntity.class, 1 )
					.add( AuditEntity.property( "component_manyToManyList" ).eq( manyToManyEntities ) )
					.getResultList();

			fail( "This should have generated an AuditException" );
		}
		catch ( Exception e ) {
			assertThat( e, instanceOf( AuditException.class ) );
			assertThat(
					e.getMessage(),
					equalTo(
							"This type of relation (org.hibernate.envers.test.integration.components.dynamic." +
									"PlainEntity.component_manyToManyList) isn't supported and can't be used in queries."
					)
			);
		}
	}

	@DynamicTest
	public void shouldFailWhenQueryOnManyToOne() {
		final PlainEntity plainEntity = (PlainEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add( AuditEntity.relatedId( "component_manyToOneEntity" ).eq( getManyToOneEntity().getId() ) )
				.getResultList().get( 0 );

		assertThat( plainEntity.getComponent().getManyToOneEntity(), equalTo( getManyToManyEntity() ) );
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	public void shouldFailWhenQueryOnOneToOne() {
		getAuditReader().createQuery()
				.forEntitiesAtRevision( PlainEntity.class, 1 )
				.add( AuditEntity.relatedId( "component_oneToOneEntity" ).eq( getOneToOneEntity().getId() ) )
				.getResultList();
	}
}
