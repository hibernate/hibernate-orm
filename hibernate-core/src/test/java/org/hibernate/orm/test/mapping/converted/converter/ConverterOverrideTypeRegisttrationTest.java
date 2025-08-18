/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.BitSet;

import org.hibernate.annotations.TypeRegistration;
import org.hibernate.orm.test.mapping.basic.bitset.BitSetHelper;
import org.hibernate.orm.test.mapping.basic.bitset.BitSetUserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <pre>
 * The @Converter should take precedence over @TypeRegistration.
 * This test shows that this is not the case.
 *
 * To ensure that the @Converter is taken into account without @TypeRegistration, you just need to remove the @TypeRegistration.
 * </pre>
 *
 * @author Vincent Bouthinon
 */
@DomainModel(
		annotatedClasses = {
				ConverterOverrideTypeRegisttrationTest.SimpleEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-19589")
public class ConverterOverrideTypeRegisttrationTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity object = new SimpleEntity( 77L );
			BitSet bitSet = new BitSet();
			bitSet.set( 0, true );
			object.setBitSet( bitSet );
			session.persist( object );
			session.flush();
			session.clear();
			SimpleEntity simpleEntity = session.find( SimpleEntity.class, object.id );
			assertThat( simpleEntity.getBitSet().get( 7 ) ).isTrue();
		} );
	}


	@Entity(name = "SimpleEntity")
	@TypeRegistration(basicClass = BitSet.class, userType = BitSetUserType.class) // Remove this annotation to test the use of @Converter
	public static class SimpleEntity {

		@Id
		private Long id;
		@Convert(converter = BitSetConverter.class)
		private BitSet bitSet;

		public SimpleEntity() {
		}

		public SimpleEntity(Long id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(final BitSet bitSet) {
			this.bitSet = bitSet;
		}
	}

	@Converter
	public static class BitSetConverter implements AttributeConverter<BitSet, String> {

		@Override
		public String convertToDatabaseColumn(final BitSet attribute) {
			return BitSetHelper.bitSetToString( attribute );
		}

		@Override
		public BitSet convertToEntityAttribute(final String dbData) {
			BitSet bitSet = new BitSet();
			bitSet.set( 7, true );
			return bitSet;
		}
	}
}
