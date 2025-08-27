/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Tests for application of filters
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				BasicFilteredBulkManipulationTest.Person.class
		}
)
public class BasicFilteredBulkManipulationTest extends AbstractStatefulStatelessFilterTest {

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testBasicFilteredHqlDelete(
			BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( session -> {
			session.persist( new Person( "Steve", 'M' ) );
			session.persist( new Person( "Emmanuel", 'M' ) );
			session.persist( new Person( "Gail", 'F' ) );
		} );
		inTransaction.accept(scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			int count = session.createQuery( "delete Person" ).executeUpdate();
			assertEquals( 2, count );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testBasicFilteredHqlUpdate(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( session -> {
			session.persist( new Person( "Shawn", 'M' ) );
			session.persist( new Person( "Sally", 'F' ) );
		} );

		inTransaction.accept(scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			int count = session.createQuery( "update Person p set p.name = 'Shawn'" ).executeUpdate();
			assertEquals( 1, count );
		} );
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Person" )
	@FilterDef(
			name = "sex",
			parameters = @ParamDef(
				name= "sexCode",
				type = Character.class
			)
	)
	@Filter( name = "sex", condition = "SEX_CODE = :sexCode" )
	public static class Person {

		@Id @GeneratedValue
		private Long id;

		private String name;

		@Column( name = "SEX_CODE" )
		private char sex;

		protected Person() {
		}

		public Person(String name, char sex) {
			this.name = name;
			this.sex = sex;
		}
	}
}
