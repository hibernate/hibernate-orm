/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-10778" )
@DomainModel( annotatedClasses = PackagePrivateAttributeConverterSessionFactoryTest.Tester.class )
@SessionFactory
public class PackagePrivateAttributeConverterSessionFactoryTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Tester tester = new Tester();
					tester.setId( 1L );
					tester.setCode( 123 );

					session.persist( tester );
				}
		);

		scope.inTransaction(
				(session) -> {
					Tuple tuple = (Tuple) session.createNativeQuery(
							"select code " +
									"from Tester " +
									"where id = :id", Tuple.class )
							.setParameter( "id", 1L )
							.getSingleResult();
					assertEquals( "123", tuple.get( "code" ) );

					Tester tester = session.find( Tester.class, 1L );
					assertEquals( 123, (int) tester.getCode() );
				}
		);
	}

	// Entity declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "Tester")
	public static class Tester {
		@Id
		private Long id;

		@Convert( converter = IntegerToVarcharConverter.class )
		private Integer code;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getCode() {
			return code;
		}

		public void setCode(Integer code) {
			this.code = code;
		}
	}

	@Converter( autoApply = true )
	static class IntegerToVarcharConverter implements AttributeConverter<Integer,String> {
		@Override
		public String convertToDatabaseColumn(Integer attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public Integer convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Integer.valueOf( dbData );
		}
	}
}
