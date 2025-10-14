/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.util.BitSet;

import org.hibernate.annotations.Type;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(annotatedClasses = {BitSetUserTypeTest.Product.class})
@SessionFactory
public class BitSetUserTypeTest {

	// Note that the following is just for legacy documentation purposes
	/*
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		//tag::basic-custom-type-register-UserType-example[]
		configuration.registerTypeContributor( (typeContributions, serviceRegistry) -> {
			typeContributions.contributeType( BitSetUserType.INSTANCE, "bitset");
		} );
		//end::basic-custom-type-register-UserType-example[]
	}
	*/

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void test(SessionFactoryScope scope) {

		BitSet bitSet = BitSet.valueOf(new long[] {1, 2, 3});

		scope.inTransaction( session -> {
			Product product = new Product();
			product.setId(1);
			product.setBitSet(bitSet);
			session.persist(product);
		});

		scope.inTransaction( session -> {
			Product product = session.find(Product.class, 1);
			assertEquals(bitSet, product.getBitSet());
		});
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		BitSet bitSet = BitSet.valueOf(new long[] {1, 2, 3});

		scope.inTransaction( session -> {
			Product product = new Product();
			product.setId(1);
			product.setBitSet(bitSet);
			session.persist(product);
		});

		scope.inTransaction( session -> {
			Product product = session.createNamedQuery(
					"find_person_by_bitset", Product.class)
					.setParameter("id", 1L)
					.getSingleResult();

			assertEquals(bitSet, product.getBitSet());
		});
	}

	@NamedNativeQuery(
		name = "find_person_by_bitset",
		query =
			"SELECT " +
			"   pr.id AS \"pr.id\", " +
			"   pr.bitset_col AS \"pr.bitset\" " +
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

		@Type(BitSetUserType.class)
		@Column(name = "bitset_col")
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
