/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SharedSessionContract;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				Animal.class, Human.class, Mammal.class
		}
)
public class FilterInheritanceTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	protected void prepareTest() throws Exception {
		scope.inTransaction( session -> {
			Mammal mammal = new Mammal();
			mammal.setName( "unimportant" );
			session.persist( mammal );

			Human human = new Human();
			human.setName( "unimportant" );
			session.persist( human );

			Human human1 = new Human();
			human1.setName( "unimportant_1" );
			session.persist( human1 );
		} );
	}

	@AfterEach
	public void tearDown() throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	@JiraKey(value = "HHH-8895")
	public void testSelectFromHuman(
			BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "nameFilter" ).setParameter( "name", "unimportant" );

			List<Human> humans = session.createQuery( "SELECT h FROM Human h", Human.class ).list();

			assertThat( humans ).hasSize( 1 );
			Human human = humans.get( 0 );
			assertThat( human.getName() ).isEqualTo( "unimportant" );
		} );
	}

}
