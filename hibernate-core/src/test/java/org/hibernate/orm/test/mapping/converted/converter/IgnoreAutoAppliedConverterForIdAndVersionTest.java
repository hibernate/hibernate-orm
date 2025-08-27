/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@JiraKey( value = "HHH-19286" )
@DomainModel( annotatedClasses = {
		IgnoreAutoAppliedConverterForIdAndVersionTest.SampleEntity.class,
		IgnoreAutoAppliedConverterForIdAndVersionTest.NegatingIntegerConverter.class,
} )
@SessionFactory
public class IgnoreAutoAppliedConverterForIdAndVersionTest {
	public final String sql = "select code from SampleEntity where id = :id";

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SampleEntity sampleEntity = new SampleEntity();
					sampleEntity.setId( 1 );
					sampleEntity.setCode( 123 );

					session.persist( sampleEntity );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Integer code = session.createNativeQuery( sql, Integer.class )
							.setParameter( "id", 1L )
							.getSingleResult();
					assertEquals( -123, (int) code );

					final SampleEntity sampleEntity = session.find( SampleEntity.class, 1 );

					assertEquals( 0, (int) sampleEntity.getVersion() );
					assertEquals( 123, (int) sampleEntity.getCode() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "SampleEntity")
	public static class SampleEntity {
		@Id
		private Integer id;
		@Version
		private Integer version;
		private Integer code;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Integer getCode() {
			return code;
		}

		public void setCode(Integer code) {
			this.code = code;
		}
	}

	@Converter( autoApply = true )
	static class NegatingIntegerConverter implements AttributeConverter<Integer,Integer> {
		@Override
		public Integer convertToDatabaseColumn(Integer attribute) {
			return attribute == null ? null : -attribute;
		}

		@Override
		public Integer convertToEntityAttribute(Integer dbData) {
			return dbData == null ? null : -dbData;
		}
	}
}
