/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

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
