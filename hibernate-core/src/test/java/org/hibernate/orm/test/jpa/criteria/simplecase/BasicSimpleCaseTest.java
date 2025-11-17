/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.simplecase;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.SimpleCase;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Mote that these are simply performing syntax checking (can the criteria query
 * be properly compiled and executed)
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {BasicSimpleCaseTest.Customer.class, BasicSimpleCaseTest.TestEntity.class})
public class BasicSimpleCaseTest {

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-9343")
	public void testCaseStringResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> query = builder.createTupleQuery();
			Root<Customer> root = query.from( Customer.class );

			Path<String> emailPath = root.get( "email" );
			CriteriaBuilder.Case<String> selectCase = builder.selectCase();
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 13 ), "Long" );
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 12 ), "Normal" );
			Expression<String> emailType = selectCase.otherwise( "Unknown" );

			query.multiselect( emailPath, emailType );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	@JiraKey(value = "HHH-9343")
	public void testCaseIntegerResult(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> query = builder.createTupleQuery();
			Root<Customer> root = query.from( Customer.class );

			Path<String> emailPath = root.get( "email" );
			CriteriaBuilder.Case<Integer> selectCase = builder.selectCase();
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 13 ), 2 );
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 12 ), 1 );
			Expression<Integer> emailType = selectCase.otherwise( 0 );

			query.multiselect( emailPath, emailType );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	@JiraKey(value = "HHH-9343")
	public void testCaseLiteralResult(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Boolean> cq = cb.createQuery( Boolean.class );
			Root<Customer> expense_ = cq.from( Customer.class );
			entityManager.createQuery(
					cq.distinct( true ).where(
							cb.equal( expense_.get( "email" ), "@hibernate.com" )
					).multiselect(
							cb.selectCase()
									.when( cb.gt( cb.count( expense_ ), cb.literal( 0L ) ), cb.literal( true ) )
									.otherwise( cb.literal( false ) )
					)
			).getSingleResult();
		} );
	}

	@Test
	@JiraKey(value = "HHH-9343")
	public void testCaseLiteralResult2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Boolean> cq = cb.createQuery( Boolean.class );
			Root<Customer> expense_ = cq.from( Customer.class );
			entityManager.createQuery(
					cq.distinct( true ).where(
							cb.equal( expense_.get( "email" ), "@hibernate.com" )
					).multiselect(
							cb.selectCase()
									.when( cb.gt( cb.count( expense_ ), cb.literal( 0L ) ), true )
									.otherwise( false )
					)
			).getSingleResult();
		} );
	}

	@Test
	public void testCaseInOrderBy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Customer> query = builder.createQuery( Customer.class );
			Root<Customer> root = query.from( Customer.class );
			query.select( root );

			Path<String> emailPath = root.get( "email" );
			SimpleCase<String, Integer> orderCase = builder.selectCase( emailPath );
			orderCase = orderCase.when( "test@test.com", 1 );
			orderCase = orderCase.when( "test2@test.com", 2 );

			query.orderBy( builder.asc( orderCase.otherwise( 0 ) ) );

			entityManager.createQuery( query );
		} );
	}

	@Test
	public void testCaseInOrderBy2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Customer> query = builder.createQuery( Customer.class );
			Root<Customer> root = query.from( Customer.class );
			query.select( root );

			Path<String> emailPath = root.get( "email" );
			SimpleCase<String, String> orderCase = builder.selectCase( emailPath );
			orderCase = orderCase.when( "test@test.com", "a" );
			orderCase = orderCase.when( "test2@test.com", "b" );

			query.orderBy( builder.asc( orderCase.otherwise( "c" ) ) );

			entityManager.createQuery( query );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13016")
	@FailureExpected(jiraKey = "HHH-13016")
	public void testCaseEnumResult(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			// create entities
			Customer customer1 = new Customer();
			customer1.setEmail( "LONG5678901234" );
			entityManager.persist( customer1 );
			Customer customer2 = new Customer();
			customer2.setEmail( "NORMAL7890123" );
			entityManager.persist( customer2 );
			Customer customer3 = new Customer();
			customer3.setEmail( "UNKNOWN" );
			entityManager.persist( customer3 );
		});
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> query = builder.createTupleQuery();
			Root<Customer> root = query.from( Customer.class );

			Path<String> emailPath = root.get( "email" );
			CriteriaBuilder.Case<EmailType> selectCase = builder.selectCase();
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 13 ), EmailType.LONG );
			selectCase.when( builder.greaterThan( builder.length( emailPath ), 12 ), EmailType.NORMAL );
			Expression<EmailType> emailType = selectCase.otherwise( EmailType.UNKNOWN );

			query.multiselect( emailPath, emailType );
			query.orderBy( builder.asc( emailPath ) );

			List<Tuple> results = entityManager.createQuery( query ).getResultList();
			assertEquals( 3, results.size() );
			assertEquals( "LONG5678901234", results.get( 0 ).get( 0 ) );
			assertEquals( EmailType.LONG, results.get( 0 ).get( 1 ) );
			assertEquals( "NORMAL7890123", results.get( 1 ).get( 0 ) );
			assertEquals( EmailType.NORMAL, results.get( 1 ).get( 1 ) );
			assertEquals( "UNKNOWN", results.get( 2 ).get( 0 ) );
			assertEquals( EmailType.UNKNOWN, results.get( 2 ).get( 1 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13199")
	public void testCaseEnumInSum(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			// create entities
			TestEntity e1 = new TestEntity();
			e1.setEnumField( TestEnum.VAL_1 );
			e1.setValue( 20L );
			entityManager.persist( e1 );

			TestEntity e2 = new TestEntity();
			e2.setEnumField( TestEnum.VAL_2 );
			e2.setValue( 10L );
			entityManager.persist( e2 );
		} );

		scope.inTransaction( entityManager -> {
			// Works in previous version (e.g. Hibernate 5.3.7.Final)
			// Fails in Hibernate 5.4.0.Final
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<TestEntity> r = query.from( TestEntity.class );

			CriteriaBuilder.Case<Long> case1 = cb.selectCase();
			case1.when( cb.equal( r.<TestEnum> get( "enumField" ), cb.literal( TestEnum.VAL_1 ) ),
					r.<Long> get( "value" ) );
			case1.otherwise( cb.nullLiteral( Long.class ) );

			CriteriaBuilder.Case<Long> case2 = cb.selectCase();
			case2.when( cb.equal( r.<TestEnum> get( "enumField" ), cb.literal( TestEnum.VAL_2 ) ),
					r.<Long> get( "value" ) );
			case2.otherwise( cb.nullLiteral( Long.class ) );

			/*
			 * Forces enums to be bound as parameters, so SQL is something like
			 * "SELECT enumfield AS enumField, SUM(CASE WHEN enumfield = ? THEN value
			 * ELSE NULL END) AS VAL_1, SUM(CASE WHEN enumfield =? THEN value ELSE NULL END) AS VAL_1 FROM TestEntity
			 * GROUP BY enumfield"
			 */
			query
					.select( cb.tuple( r.<TestEnum> get( "enumField" ).alias( "enumField" ),
							cb.sum( case1 ).alias( "VAL_1" ),
							cb.sum( case2 ).alias( "VAL_2" ) ) )
					.groupBy( r.<TestEnum> get( "enumField" ) );

			List<Tuple> list = entityManager.createQuery( query ).getResultList();
			assertEquals( 2, list.size() );
			for ( Tuple tuple : list ) {
				TestEnum enumVal = tuple.get( "enumField", TestEnum.class );
				if ( enumVal == TestEnum.VAL_1 ) {
					assertEquals( 20L, tuple.get( "VAL_1", Long.class ).longValue() );
					assertNull( tuple.get( "VAL_2", Long.class ) );
				}
				else if ( enumVal == TestEnum.VAL_2 ) {
					assertNull( tuple.get( "VAL_1", Long.class ) );
					assertEquals( 10L, tuple.get( "VAL_2", Long.class ).longValue() );
				}
			}
		} );
	}

	@Entity(name = "Customer")
	@Table(name = "customer")
	public static class Customer {
		private Integer id;
		private String email;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

	public enum EmailType {
		LONG, NORMAL, UNKNOWN
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;

		@Convert(converter = TestEnumConverter.class)
		private TestEnum enumField;

		@Column(name = "val")
		private Long value;

		public long getId() {
			return id;
		}

		public TestEnum getEnumField() {
			return enumField;
		}

		public void setEnumField(TestEnum enumField) {
			this.enumField = enumField;
		}

		public Long getValue() {
			return value;
		}

		public void setValue(Long value) {
			this.value = value;
		}
	}

	public static enum TestEnum {
		VAL_1("1"),
		VAL_2("2");

		private final String value;

		private TestEnum(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static TestEnum fromValue(String value) {
			if (value.equals(VAL_1.value)) {
				return VAL_1;
			} else if (value.equals(VAL_2.value)) {
				return VAL_2;
			}
			return null;
		}
	}

	public static class TestEnumConverter implements AttributeConverter<TestEnum, String> {
		@Override
		public String convertToDatabaseColumn(TestEnum attribute) {
			return attribute == null ? null : attribute.getValue();
		}

		@Override
		public TestEnum convertToEntityAttribute(String dbData) {
			return dbData == null ? null : TestEnum.fromValue(dbData);
		}
	}
}
