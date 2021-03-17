/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BitSetUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Override
	protected Configuration constructAndConfigureConfiguration() {
		Configuration configuration = super.constructAndConfigureConfiguration();
		//tag::basic-custom-type-register-UserType-example[]
		configuration.registerTypeContributor( (typeContributions, serviceRegistry) -> {
			typeContributions.contributeType( BitSetUserType.INSTANCE, "bitset");
		} );
		//end::basic-custom-type-register-UserType-example[]
		return configuration;
	}

	@Test
	public void test() {

		BitSet bitSet = BitSet.valueOf( new long[] {1, 2, 3} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = new Product( );
			product.setId( 1 );
			product.setBitSet( bitSet );
			session.persist( product );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = session.get( Product.class, 1 );
			assertEquals(bitSet, product.getBitSet());
		} );
	}

	@Test
	public void testNativeQuery() {
		BitSet bitSet = BitSet.valueOf( new long[] {1, 2, 3} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = new Product( );
			product.setId( 1 );
			product.setBitSet( bitSet );
			session.persist( product );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = (Product) session.getNamedNativeQuery(
					"find_person_by_bitset")
					.setParameter( "id", 1L)
					.getSingleResult();

			assertEquals(bitSet, product.getBitSet());
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@NamedNativeQuery(
		name = "find_person_by_bitset",
		query =
			"SELECT " +
			"   pr.id AS \"pr.id\", " +
			"   pr.bitset AS \"pr.bitset\" " +
			"FROM Product pr " +
			"WHERE pr.id = :id",
		resultSetMapping = "Person"
	)
	@SqlResultSetMapping(
		name = "Person",
		classes = @ConstructorResult(
			targetClass = Product.class,
			columns = {
				@ColumnResult(name = "pr.id"),
				@ColumnResult(name = "pr.bitset", type = BitSetUserType.class)
			}
		)
	)
	//tag::basic-custom-type-BitSetUserType-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		@Type( type = "bitset" )
		private BitSet bitSet;

		//Constructors, getters, and setters are omitted for brevity
	//end::basic-custom-type-BitSetUserType-mapping-example[]
		public Product() {
		}

		public Product(Number id, BitSet bitSet) {
			this.id = id.intValue();
			this.bitSet = bitSet;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
	//tag::basic-custom-type-BitSetUserType-mapping-example[]
	}
	//end::basic-custom-type-BitSetUserType-mapping-example[]
}
