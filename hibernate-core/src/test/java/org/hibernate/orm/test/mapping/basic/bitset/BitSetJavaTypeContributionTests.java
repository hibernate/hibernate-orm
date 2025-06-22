/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.util.BitSet;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = BitSetJavaTypeContributionTests.Product.class,
		typeContributors = BitSetJavaTypeContributor.class
)
@SessionFactory
public class BitSetJavaTypeContributionTests {

	@Test
	public void testResolution(SessionFactoryScope scope) {
		final EntityPersister productType = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor(Product.class);
		final SingularAttributeMapping bitSetAttribute = (SingularAttributeMapping) productType.findAttributeMapping("bitSet");
		// make sure BitSetTypeDescriptor was selected
		assertThat( bitSetAttribute.getJavaType(), instanceOf( BitSetJavaType.class));
	}


	@Table(name = "Product")
	//tag::basic-bitset-example-java-type-contrib[]
	@Entity(name = "Product")
	public static class Product {
		@Id
		private Integer id;

		private BitSet bitSet;

		//Constructors, getters, and setters are omitted for brevity
		//end::basic-bitset-example-java-type-contrib[]
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
		//tag::basic-bitset-example-java-type-contrib[]
	}
	//end::basic-bitset-example-java-type-contrib[]
}
