/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.util.Date;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8807" )
@DomainModel(annotatedClasses = {ExplicitDateConvertersTest.Entity1.class})
@SessionFactory
public class ExplicitDateConvertersTest {

	// NOTE : initially unable to reproduce the reported problem

	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	public static class LongToDateConverter implements AttributeConverter<Date,Long> {
		@Override
		public Long convertToDatabaseColumn(Date attribute) {
			convertToDatabaseColumnCalled = true;
			return attribute.getTime();
		}

		@Override
		public Date convertToEntityAttribute(Long dbData) {
			convertToEntityAttributeCalled = true;
			return new Date( dbData );
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
		@Convert( converter = LongToDateConverter.class )
		private Date theDate;

		public Entity1() {
		}

		public Entity1(Integer id, String name, Date theDate) {
			this.id = id;
			this.name = name;
			this.theDate = theDate;
		}
	}

	@Test
	public void testSimpleConvertUsage(SessionFactoryScope scope) {
		final EntityPersister ep = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Entity1.class.getName());
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final ConvertedBasicTypeImpl type = assertTyping(
				ConvertedBasicTypeImpl.class,
				theDatePropertyType
		);
		final JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
		assertTrue( LongToDateConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

		resetFlags();

		scope.inTransaction( session -> session.persist(new Entity1(1, "1", new Date())) );
		assertTrue( convertToDatabaseColumnCalled );

		resetFlags();

		scope.inTransaction( session -> session.find( Entity1.class, 1 ) );
		assertTrue( convertToEntityAttributeCalled );

		scope.dropData();
	}
}
