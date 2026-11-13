package org.hibernate.orm.test.query.hql.instantiation;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = InstantiationWithMultipleWrapperConstructorsTest.Product.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17515" )
public class InstantiationWithMultipleWrapperConstructorsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Product( 1, (short) 11, "111" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Product" ).executeUpdate() );
	}

	@Test
	public void testIntegerAndString(SessionFactoryScope scope) {
		dynamicInstatiation( scope, "id", "name", 1, "111" );
	}

	@Test
	public void testShortAndString(SessionFactoryScope scope) {
		dynamicInstatiation( scope, "productNo", "name", 11, "111" );
	}

	@Test
	public void testShortAndShort(SessionFactoryScope scope) {
		dynamicInstatiation( scope, "productNo", "productNo", 11, "11" );
	}

	@Test
	public void testIntegerAndShort(SessionFactoryScope scope) {
		dynamicInstatiation( scope, "id", "productNo", 1, "11" );
	}

	@Test
	public void testIntegerAndInteger(SessionFactoryScope scope) {
		dynamicInstatiation( scope, "id", "id", 1, "1" );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17926" )
	void reportsSignatureWhenNoOverloadMatches(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var failure = assertThrows( SemanticException.class, () -> session.createSelectionQuery(
					"select new " + KeyValue.class.getName() + "(p.id, true) from Product p", KeyValue.class
			) );
			assertThat( failure )
					.hasMessageContaining( "(java.lang.Integer, java.lang.Boolean)" )
					.hasMessageNotContaining( "(argument " );
		} );
	}

	private void dynamicInstatiation(SessionFactoryScope scope, String prop1, String prop2, Integer key, String value) {
		scope.inTransaction( session -> {
			final KeyValue result1 = session.createQuery( String.format(
					"select new %s(p.%s, p.%s) from Product p",
					KeyValue.class.getName(),
					prop1,
					prop2
			), KeyValue.class ).getSingleResult();
			assertThat( result1.getKey() ).isEqualTo( key );
			assertThat( result1.getValue() ).isEqualTo( value );
			final KeyValuePrimitive result2 = session.createQuery( String.format(
					"select new %s(p.%s, p.%s) from Product p",
					KeyValuePrimitive.class.getName(),
					prop1,
					prop2
			), KeyValuePrimitive.class ).getSingleResult();
			assertThat( result2.getKey() ).isEqualTo( key );
			assertThat( result2.getValue() ).isEqualTo( value );
		} );
	}

	@Entity( name = "Product" )
	public static class Product {
		@Id
		private Integer id;

		private Short productNo;

		private String name;

		public Product() {
		}

		public Product(Integer id, Short productNo, String name) {
			this.id = id;
			this.productNo = productNo;
			this.name = name;
		}
	}

	@SuppressWarnings( "unused" )
	public static class KeyValue {
		private final Integer key;

		private final String value;

		public KeyValue(Integer k, String val) {
			key = k;
			value = val;
		}

		public KeyValue(Short k, String val) {
			key = k == null ? null : k.intValue();
			value = val;
		}

		public KeyValue(Short k, Short val) {
			key = k == null ? null : k.intValue();
			value = String.valueOf( val );
		}

		public KeyValue(Integer k, Short val) {
			key = k;
			value = String.valueOf( val );
		}

		public KeyValue(Integer k, Integer val) {
			key = k;
			value = String.valueOf( val );
		}

		public Integer getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}

	@SuppressWarnings( "unused" )
	public static class KeyValuePrimitive {
		private final Integer key;

		private final String value;

		// primitive int should be compatible with both Short and Integer
		public KeyValuePrimitive(int k, String val) {
			key = k;
			value = val;
		}

		// primitive int should be compatible with both Short and Integer
		public KeyValuePrimitive(int k, int val) {
			key = k;
			value = String.valueOf( val );
		}

		public Integer getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}
