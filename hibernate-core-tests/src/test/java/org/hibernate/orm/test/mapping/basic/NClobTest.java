package org.hibernate.orm.test.mapping.basic;

import java.sql.Clob;
import java.sql.NClob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.proxy.NClobProxy;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = NClobTest.Product.class)
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsNationalizedDataTypes.class,
		comment = "This is different from other tests checking generalized nationalization support; " +
				"because we explicitly map this attribute to the `NClob` java type the database really" +
				" has to support those types"
)
@SkipForDialect(dialectClass = SybaseASEDialect.class)
public class NClobTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@JiraKey( "HHH-14296" )
	@RequiresDialect( OracleDialect.class )
	public void testNativeQueryNClobAutoDiscovery(EntityManagerFactoryScope scope) {
		final String warranty = "My product®™ warranty 😍";
		scope.inTransaction( entityManager -> {
			final Product product = new Product();
			product.setId( 2 );
			product.setName( "Mobile phone" );
			product.setWarranty( NClobProxy.generateProxy( warranty ) );
			entityManager.persist( product );
		} );

		scope.inTransaction( entityManager -> {
			final Object result = entityManager.createNativeQuery( "select warranty from Product where id = 2" )
					.getSingleResult();
			final Clob clob = assertInstanceOf( Clob.class, result );
			assertEquals( warranty, assertDoesNotThrow( () -> clob.getSubString( 1, (int) clob.length() ) ) );
		} );
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					//tag::basic-nclob-persist-example[]
					String warranty = "My product®™ warranty 😍";

					final Product product = new Product();
					product.setId(1);
					product.setName("Mobile phone");

					product.setWarranty(NClobProxy.generateProxy(warranty));

					entityManager.persist(product);
					//end::basic-nclob-persist-example[]
				}
	);

		scope.inTransaction(
				(entityManager) -> {
					try {
						//tag::basic-nclob-find-example[]
						Product product = entityManager.find(Product.class, 1);

						NClob warranty = product.getWarranty();
						assertEquals("My product®™ warranty 😍", warranty.getSubString( 1, (int) warranty.length() ) );
						//end::basic-nclob-find-example[]
					}
					catch (Exception e) {
						fail(e.getMessage());
					}
				}
		);
	}

	//tag::basic-nclob-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		@Lob
		@Nationalized
		// Clob also works, because NClob extends Clob.
		// The database type is still NCLOB either way and handled as such.
		private NClob warranty;

		//Getters and setters are omitted for brevity

		//end::basic-nclob-example[]
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public NClob getWarranty() {
			return warranty;
		}

		public void setWarranty(NClob warranty) {
			this.warranty = warranty;
		}

		//tag::basic-nclob-example[]
	}
	//end::basic-nclob-example[]
}
