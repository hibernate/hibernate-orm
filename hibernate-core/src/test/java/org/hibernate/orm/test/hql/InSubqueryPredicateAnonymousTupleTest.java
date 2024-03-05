/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.hql;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEntity.class,
		InSubqueryPredicateAnonymousTupleTest.TestEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17332" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17701" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17803" )
public class InSubqueryPredicateAnonymousTupleTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "test" ) );
			session.persist( new TestEntity( 1L, new Money( 100L ), Status.VALID ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testSimpleInSubqueryPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select sub.data from (select e.id id, e.data data from BasicEntity e) sub" +
							" where sub.data in (select e.data from BasicEntity e)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "test" );
		} );
	}

	@Test
	public void testTupleInSubqueryPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// note : without cast(sub.data as string) Sybase jTDS fails with "TDS Protocol error: Invalid TDS data type"
			final String result = session.createQuery(
					"select cast(sub.data as string) from (select e.id id, e.data data from BasicEntity e) sub" +
							" where (sub.id, sub.data) in (select e.id, e.data from BasicEntity e)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "test" );
		} );
	}

	@Test
	public void testConvertedAttributeTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"select t from TestEntity t where (t.id, t.money) in " +
							"(select t2.id, t2.money from TestEntity t2)",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getMoney().getCents() ).isEqualTo( 100L );
		} );
	}

	@Test
	public void testEnumeratedAttributeTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"select t from TestEntity t where (t.id, t.status) in " +
								"(select t2.id, t2.status from TestEntity t2)",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getStatus() ).isEqualTo( Status.VALID );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Long id;

		@Convert( converter = MoneyConverter.class )
		private Money money;

		@Enumerated( EnumType.STRING )
		private Status status;

		public TestEntity() {
		}

		public TestEntity(Long id, Money money, Status status) {
			this.id = id;
			this.money = money;
			this.status = status;
		}

		public Money getMoney() {
			return money;
		}

		public Status getStatus() {
			return status;
		}
	}

	public enum Status {
		VALID, INVALID
	}

	public static class Money {
		private long cents;

		public Money(long cents) {
			this.cents = cents;
		}

		public long getCents() {
			return cents;
		}
	}

	@Converter
	public static class MoneyConverter implements AttributeConverter<Money, Long> {
		@Override
		public Long convertToDatabaseColumn(Money attribute) {
			return attribute == null ? null : attribute.getCents();
		}

		@Override
		public Money convertToEntityAttribute(Long dbData) {
			return dbData == null ? null : new Money( dbData );
		}
	}
}
