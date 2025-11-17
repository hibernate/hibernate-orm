/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@DomainModel(
		annotatedClasses = {
				BatchAndEnumIdTest.Property.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2")
		}
)
@SessionFactory
@JiraKey("HHH-16639")
public class BatchAndEnumIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Property property1 = new Property( Property.Key.KEY1, "value1" );
					Property property2 = new Property( Property.Key.KEY2, "value2" );
					session.persist( property1 );
					session.persist( property2 );
				}
		);
	}

	@AfterEach
	public void tearDowm(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Property property = session.getReference( Property.class, Property.Key.KEY1 );

					property.setValue( "value1 updated" );
					session.merge( property );
				}
		);
	}

	@Test
	public void testGetReference2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Property property = session.getReference( Property.class, Property.Key.KEY1 );
					Property property2 = session.getReference( Property.class, Property.Key.KEY2 );

					property.setValue( "value1 updated" );
					session.merge( property );

					property2.setValue( "value2 updated" );
					session.merge( property2 );
				}
		);
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Property property = session.find( Property.class, Property.Key.KEY1 );

					property.setValue( "value1 updated" );
					session.merge( property );
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Property property = session.find( Property.class, Property.Key.KEY1 );
					Property property2 = session.find( Property.class, Property.Key.KEY2 );

					property.setValue( "value1 updated" );
					session.merge( property );

					property2.setValue( "value2 updated" );
					session.merge( property2 );
				}
		);
	}

	@Entity(name = "Property")
	@Table(name = "PROPERTY_TABLE")
	public static class Property {

		public enum Key {
			KEY1,
			KEY2,
			KEY3
		}

		public Property() {
		}

		public Property(Key key, String value) {
			this.key = key;
			this.value = value;
		}

		@Id
		@Column(name = "KEY_COLUMN")
		@Enumerated(EnumType.STRING)
		private Key key;

		@Column(name = "VALUE_COLUMN")
		private String value;

		public Key getKey() {
			return key;
		}

		public void setKey(Key key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
