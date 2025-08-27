/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.secondarytable;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DomainModel(
		annotatedClasses = {
				User.class
		}
)
public class SecondaryTableTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	void prepareTest() {
		scope.inTransaction( s -> {
			insertUser( s, "q@s.com", 21, false, "a1", "b" );
			insertUser( s, "r@s.com", 22, false, "a2", "b" );
			insertUser( s, "s@s.com", 23, true, "a3", "b" );
			insertUser( s, "t@s.com", 24, false, "a4", "b" );
		} );
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilter(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
/*			Assert.assertEquals(
					4L,
					session.createQuery( "select count(u) from User u" ).uniqueResult()
			);*/
			session.enableFilter( "ageFilter" ).setParameter( "age", 24 );
			assertThat(
					session.createQuery( "select count(u) from User u" ).uniqueResult()
			).isEqualTo( 2L );
		} );
	}

	private void insertUser(
			Session session,
			String emailAddress,
			int age,
			boolean lockedOut,
			String username,
			String password) {
		User user = new User();
		user.setEmailAddress( emailAddress );
		user.setAge( age );
		user.setLockedOut( lockedOut );
		user.setUsername( username );
		user.setPassword( password );
		session.persist( user );
	}

}
