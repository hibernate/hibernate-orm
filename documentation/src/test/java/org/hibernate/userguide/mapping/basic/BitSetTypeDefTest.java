/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TypeDef;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@DomainModel( annotatedClasses = BitSetTypeDefTest.Product.class )
@SessionFactory
public class BitSetTypeDefTest {

	@Test
	public void test(SessionFactoryScope scope) {

		//tag::basic-custom-type-BitSetTypeDef-persistence-example[]
		BitSet bitSet = BitSet.valueOf( new long[] {1, 2, 3} );

		scope.inTransaction(
				(session) -> {
					Product product = new Product( );
					product.setId( 1 );
					product.setBitSet( bitSet );
					session.persist( product );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Product product = session.get( Product.class, 1 );
					assertEquals(bitSet, product.getBitSet());
				}
		);
		//end::basic-custom-type-BitSetTypeDef-persistence-example[]
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Product" ).executeUpdate()
		);
	}

	//tag::basic-custom-type-BitSetTypeDef-mapping-example[]
	@Entity(name = "Product")
	@TypeDef(
		name = "bitset",
		defaultForType = BitSet.class,
		typeClass = BitSetType.class
	)
	public static class Product {

		@Id
		private Integer id;

		private BitSet bitSet;

		//Getters and setters are omitted for brevity
	//end::basic-custom-type-BitSetTypeDef-mapping-example[]

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
	//tag::basic-custom-type-BitSetTypeDef-mapping-example[]
	}
	//end::basic-custom-type-BitSetTypeDef-mapping-example[]
}
