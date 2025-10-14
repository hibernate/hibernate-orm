/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.util.BitSet;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.usertype.UserTypeLegacyBridge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {BitSetTypeTest.Product.class},
		typeContributors = {BitSetTypeContributor.class}
)
@SessionFactory
@ServiceRegistry()
public class BitSetTypeTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void test(SessionFactoryScope scope) {

		//tag::basic-custom-type-BitSetType-persistence-example[]
		BitSet bitSet = BitSet.valueOf( new long[] {1, 2, 3} );

		scope.inTransaction( session -> {
			Product product = new Product( );
			product.setId( 1 );
			product.setBitSet( bitSet );
			session.persist( product );
		} );

		scope.inTransaction( session -> {
			Product product = session.find( Product.class, 1 );
			assertEquals(bitSet, product.getBitSet());
		} );
		//end::basic-custom-type-BitSetType-persistence-example[]
	}

	//tag::basic-custom-type-BitSetType-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		@Type(
				value = UserTypeLegacyBridge.class,
				parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "bitset")
		)
		private BitSet bitSet;

		public Integer getId() {
			return id;
		}

		//Getters and setters are omitted for brevity
	//end::basic-custom-type-BitSetType-mapping-example[]

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
	//tag::basic-custom-type-BitSetType-mapping-example[]
	}
	//end::basic-custom-type-BitSetType-mapping-example[]
}
