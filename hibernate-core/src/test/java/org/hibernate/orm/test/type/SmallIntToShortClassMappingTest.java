/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@RequiresDialect(H2Dialect.class)
public class SmallIntToShortClassMappingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Event.class,
		};
	}

	@Test
	@JiraKey(value = "HHH-12115")
	public void testShortType() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.id = 1;
			event.registrationNumber = "123";

			entityManager.persist( event );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.find( Event.class, (short) 1 );
			assertEquals( "123", event.registrationNumber );
		} );
	}

	@Entity(name = "Event")
	@Table(name = "event")
	public static class Event {

		@Id
		@Column(columnDefinition = "SMALLINT")
		private Short id;

		@Column(columnDefinition = "SMALLINT")
		@Convert(converter = ShortToString.class)
		private String registrationNumber;
	}

	@Converter
	public static class ShortToString implements AttributeConverter<String, Short> {
		@Override
		public Short convertToDatabaseColumn(String attribute) {
			if ( attribute == null ) {
				return null;
			}

			return Short.valueOf( attribute );
		}

		@Override
		public String convertToEntityAttribute(Short dbData) {
			if ( dbData == null ) {
				return null;
			}

			return String.valueOf( dbData );
		}
	}
}
