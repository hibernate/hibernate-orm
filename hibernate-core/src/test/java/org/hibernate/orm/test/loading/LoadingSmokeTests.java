/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import org.hibernate.Hibernate;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.persister.entity.AbstractEntityPersister;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
@SessionFactoryFunctionalTesting
public class LoadingSmokeTests {
	@Test
	public void testBasicLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final BasicEntity loaded = session.byId( BasicEntity.class ).getReference( 1 );
					assertThat( loaded, notNullValue() );
					assertThat( Hibernate.isInitialized( loaded ), is( false ) );
				}
		);
	}

	@Test
	public void testBasicGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final BasicEntity gotten = session.byId( BasicEntity.class ).load( 1 );
					assertThat( gotten, notNullValue() );
					assertThat( Hibernate.isInitialized( gotten ), is( true ) );
					assertThat( gotten.getId(), is( 1 ) );
					assertThat( gotten.getData(), is( "first" ) );

					final AbstractEntityPersister entityDescriptor = (AbstractEntityPersister) session.getSessionFactory()
							.getRuntimeMetamodels().getMappingMetamodel()
							.getEntityDescriptor( BasicEntity.class );

					final SingleIdEntityLoader singleIdEntityLoader = entityDescriptor.getSingleIdLoader();
					assertThat( singleIdEntityLoader, instanceOf( SingleIdEntityLoaderStandardImpl.class ) );
				}
		);
	}

	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new BasicEntity( 1, "first" ) );
					session.persist( new BasicEntity( 2, "second" ) );
				}
		);
	}

	@AfterAll
	public void deleteTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.doWork(
							connection -> {
								connection.prepareStatement( "delete from BasicEntity" ).execute();
							}
					);
				}
		);
	}
}
