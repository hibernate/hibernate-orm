/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.BitSet;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;


public class BitSetMetaUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Test
	public void test() {

		BitSet bitSet = BitSet.valueOf(new long[] {1, 2, 3});

		doInHibernate(this::sessionFactory, session -> {
			Product product = new Product();
			product.setId(1);
			product.setBitSet(bitSet);
			session.persist(product);
		});

		doInHibernate(this::sessionFactory, session -> {
			Product product = session.get(Product.class, 1);
			assertEquals(bitSet, product.getBitSet());
		});
	}

	@Test
	public void testNativeQuery() {
		BitSet bitSet = BitSet.valueOf(new long[] {1, 2, 3});

		doInHibernate(this::sessionFactory, session -> {
			Product product = new Product();
			product.setId(1);
			product.setBitSet(bitSet);
			session.persist(product);
		});

		doInHibernate(this::sessionFactory, session -> {
			Product product = session.createNamedQuery(
					"find_person_by_bitset", Product.class)
					.setParameter("id", 1L)
					.getSingleResult();

			assertEquals(bitSet, product.getBitSet());
		});
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Type(BitSetUserType.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface BitSetType {}

	@NamedNativeQuery(
		name = "find_person_by_bitset",
		query =
			"SELECT " +
			"   pr.id AS \"pr.id\", " +
			"   pr.bitset_col AS \"pr.bitset\" " +
			"FROM products pr " +
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
	@Entity(name = "Product")
	@Table(name = "products")
	public static class Product {

		@Id
		private Integer id;

		@BitSetType
		@Column(name = "bitset_col")
		private BitSet bitSet;

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
	}
}
