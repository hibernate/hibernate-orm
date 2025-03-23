/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEntity.class,
		InSubqueryPredicateAnonymousTupleTest.TestEntity.class,
		InSubqueryPredicateAnonymousTupleTest.MarketSale.class,
		InSubqueryPredicateAnonymousTupleTest.Peach.class
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
		scope.inTransaction( session -> session.createMutationQuery( "delete from MarketSale" ).executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from Peach" ).executeUpdate() );
	}

	@Test
	public void testSimpleInSubqueryPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select sub.data from (select e.id id, e.data data from BasicEntity e) sub" +
							" where sub.data in (select e.data from BasicEntity e)",
					String.class
			).getSingleResult();
			assertEquals( "test", result );
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
			assertEquals( "test", result );
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
			assertEquals( 100L, result.getMoney().getCents() );
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
			assertEquals( Status.VALID, result.getStatus() );
		} );
	}

	@Test
	@Jira(value = "https://hibernate.atlassian.net/browse/HHH-17630")
	public void testTupleInSubqueryPredicate2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Peach(1, "Green peach", FruitColor.GREEN) );
					session.persist( new MarketSale(1, "Green peach", FruitColor.GREEN) );

					int count = session.createMutationQuery( "delete from MarketSale m where "
							+ "(m.fruitColor, m.fruitName) in (select p.color, p.name from Peach p)" ).executeUpdate();
					assertEquals( 1, count );
				}
		);

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
	@Entity(name = "MarketSale")
	public static class MarketSale
	{
		@Id
		private Integer id;
		private String fruitName;
		@Enumerated(EnumType.STRING)
		private FruitColor fruitColor;

		public MarketSale() {
		}

		public MarketSale(Integer id, String fruitName, FruitColor fruitColor) {
			this.id = id;
			this.fruitName = fruitName;
			this.fruitColor = fruitColor;
		}
	}

	@Entity(name = "Peach")
	public static class Peach
	{
		@Id
		private Integer id;
		private String name;
		@Enumerated(EnumType.STRING)
		private FruitColor color;

		public Peach() {
		}

		public Peach(Integer id, String name, FruitColor color) {
			this.id = id;
			this.name = name;
			this.color = color;
		}
	}

	public enum FruitColor
	{
		GREEN
	}

}
