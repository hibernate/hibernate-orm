/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = { CharacterColumnTest.MyEntity.class })
public class CharacterColumnTest {

	@Test
	public void queryTest(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					final var list = session.createQuery( "from MyEntity where status='A'", MyEntity.class )
							.getResultList();
					assertTrue( list.isEmpty() );
				}
		);
	}

	public static enum Status {
		OK( 'O' ),
		ERROR( 'E' );

		private final char value;

		Status(char value) {
			this.value = value;
		}

		public char value() {
			return value;
		}
	}

	@Converter(autoApply = true)
	public static class StatusAttributeConverter implements AttributeConverter<Status, Character> {

		@Override
		public Character convertToDatabaseColumn(final Status attribute) {
			return attribute == null ? null : attribute.value();
		}

		@Override
		public Status convertToEntityAttribute(final Character dbData) {
			return dbData == null ? null : dbData == 'O' ? Status.OK : Status.ERROR;
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue
		public Long id;

		@Column(length = 1)
		@Convert(converter = StatusAttributeConverter.class)
		public Status status;
	}
}
