/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Product_;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = {
		Product.class,
		ShelfLife.class,
		CastTest.TestEntity.class
},
		useCollectingStatementInspector = true
)
public class CastTest {
	private static final int QUANTITY = 2;
	private static final String NAME = "a";
	private static final Integer NAME_CONVERTED_VALUE = 1;

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Product product = new Product();
					product.setId( "product1" );
					product.setPrice( 1.23d );
					product.setQuantity( QUANTITY );
					product.setPartNumber( ( (long) Integer.MAX_VALUE ) + 1 );
					product.setRating( 1.999f );
					product.setSomeBigInteger( BigInteger.valueOf( 987654321 ) );
					product.setSomeBigDecimal( BigDecimal.valueOf( 987654.321 ) );
					entityManager.persist( product );

					TestEntity testEntity = new TestEntity( 1, NAME );
					entityManager.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Product" ).executeUpdate();
					entityManager.createQuery( "delete from TestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-5755")
	public void testCastToString(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> root = criteria.from( Product.class );
					criteria.where( builder.equal(
							root.get( Product_.quantity ).as( String.class ),
							builder.literal( String.valueOf( QUANTITY ) )
					) );
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					Assertions.assertEquals( 1, result.size() );

					assertExecuteQueryContainsACast( statementInspector );
				}
		);
	}

	@Test
	@JiraKey( "HHH-15713")
	public void testCastIntegerToIntegreDoesNotCreateACast(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = builder.createQuery( Product.class );
					Root<Product> root = criteria.from( Product.class );
					criteria.where( builder.equal(
											root.get( Product_.quantity ).as( Integer.class ),
											builder.literal( QUANTITY )
									)
					);
					List<Product> result = entityManager.createQuery( criteria ).getResultList();
					Assertions.assertEquals( 1, result.size() );

					assertExecuteQueryDoesNotContainACast( statementInspector );
				}
		);
	}

	@Test
	@JiraKey( "HHH-15725")
	public void testCastAndConvertedAttribute(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<TestEntity> criteria = builder.createQuery( TestEntity.class );
					Root<TestEntity> root = criteria.from( TestEntity.class );
					criteria.where( builder.equal(
											root.get( "name" ).as( Integer.class ),
											builder.literal( NAME_CONVERTED_VALUE )
									)
					);
					List<TestEntity> result = entityManager.createQuery( criteria ).getResultList();
					Assertions.assertEquals( 1, result.size() );

					assertExecuteQueryDoesNotContainACast( statementInspector );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<TestEntity> criteria = builder.createQuery( TestEntity.class );
					Root<TestEntity> root = criteria.from( TestEntity.class );
					criteria.where( builder.equal(
											root.get( "name" ).as( String.class ),
											builder.literal( "1" )
									)
					);
					List<TestEntity> result = entityManager.createQuery( criteria ).getResultList();
					Assertions.assertEquals( 1, result.size() );

					assertExecuteQueryContainsACast( statementInspector );
				}
		);
	}

	private static void assertExecuteQueryContainsACast(SQLStatementInspector statementInspector) {
		assertTrue( getExecutedQuery( statementInspector ).contains( "cast" ) );
	}

	private static void assertExecuteQueryDoesNotContainACast(SQLStatementInspector statementInspector) {
		assertFalse( getExecutedQuery( statementInspector ).contains( "cast" ) );
	}

	private static String getExecutedQuery(SQLStatementInspector statementInspector) {
		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );

		String executedQuery = sqlQueries.get( 0 );
		return executedQuery;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@Convert(converter = TestConverter.class)
		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	private static class TestConverter implements AttributeConverter<String, Integer> {
		@Override
		public Integer convertToDatabaseColumn(String attribute) {
			if ( attribute.equals( NAME ) ) {
				return NAME_CONVERTED_VALUE;
			}
			return 0;
		}

		@Override
		public String convertToEntityAttribute(Integer dbData) {
			if ( dbData == 1 ) {
				return "a";
			}
			return "b";
		}
	}

}
