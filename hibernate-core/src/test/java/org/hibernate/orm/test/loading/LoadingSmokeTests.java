/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import org.hamcrest.MatcherAssert;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
@SessionFactoryFunctionalTesting
@SuppressWarnings("WeakerAccess")
@Tags({
	@Tag("RunnableIdeTest"),
})
public class LoadingSmokeTests {
	@Test
	public void testBasicLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final BasicEntity loaded = session.byId( BasicEntity.class ).getReference( 1 );
					assertThat( loaded, notNullValue() );
					assertFalse( Hibernate.isInitialized( loaded ) );
				}
		);
	}

	@Test
	@FailureExpected( reason = "read-by-position not yet implemented for loading" )
	public void testBasicGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final BasicEntity gotten = session.byId( BasicEntity.class ).load( 1 );
					assertThat( gotten, notNullValue() );
					assertTrue( Hibernate.isInitialized( gotten ) );
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
