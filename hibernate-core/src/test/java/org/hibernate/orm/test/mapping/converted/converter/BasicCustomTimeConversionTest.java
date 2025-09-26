/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.net.MalformedURLException;
import java.time.LocalDate;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;


import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8842" )
public class BasicCustomTimeConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	public record CustomLocalDate(int year,int month,int day) { }

	public static class CustomLocalDateConverter implements AttributeConverter<CustomLocalDate, LocalDate> {
		public LocalDate convertToDatabaseColumn(CustomLocalDate customLocalDate) {
			convertToDatabaseColumnCalled = true;
			return LocalDate.of(customLocalDate.year(),customLocalDate.month(), customLocalDate.day() );
		}

		public CustomLocalDate convertToEntityAttribute(LocalDate date) {
			convertToEntityAttributeCalled = true;
			return new CustomLocalDate( date.getYear(), date.getMonthValue(), date.getDayOfMonth());
		}
	}

	@Entity( name = "TheEntity" )
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert( converter = CustomLocalDateConverter.class )
		public CustomLocalDate theDate;

		public TheEntity() {
		}

		public TheEntity(Integer id, CustomLocalDate theDate) {
			this.id = id;
			this.theDate = theDate;
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getMappingMetamodel().getEntityDescriptor(TheEntity.class.getName());
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final ConvertedBasicTypeImpl type = assertTyping( ConvertedBasicTypeImpl.class, theDatePropertyType );
		final JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
		assertTrue( CustomLocalDateConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

		resetFlags();

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new TheEntity( 1, new CustomLocalDate( 2025, 9, 26 ) ) );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToDatabaseColumnCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.get( TheEntity.class, 1 );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToEntityAttributeCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete TheEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
