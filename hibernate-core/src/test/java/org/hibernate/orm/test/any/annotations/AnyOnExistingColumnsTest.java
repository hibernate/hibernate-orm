/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AnyOnExistingColumnsTest.IntegerProperty.class,
		AnyOnExistingColumnsTest.StringProperty.class,
		AnyOnExistingColumnsTest.PropertyHolder.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16918" )
public class AnyOnExistingColumnsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IntegerProperty ageProperty = new IntegerProperty( 1L, "age", 23 );
			session.persist( ageProperty );
			final PropertyHolder agePropertyHolder = new PropertyHolder( 1L, "I", ageProperty );
			session.persist( agePropertyHolder );
			final StringProperty nameProperty = new StringProperty( 2L, "name", "John Doe" );
			session.persist( nameProperty );
			final PropertyHolder namePropertyHolder = new PropertyHolder( 2L, "S", nameProperty );
			session.persist( namePropertyHolder );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PropertyHolder" ).executeUpdate();
			session.createMutationQuery( "delete from " + Property.class.getName() ).executeUpdate();
		} );
	}

	@Test
	public void testFindIntegerPropertyHolder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyHolder propertyHolder = session.find( PropertyHolder.class, 1L );
			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "age" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( 23 );
		} );
	}

	@Test
	public void testFindStringPropertyHolder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyHolder propertyHolder = session.find( PropertyHolder.class, 2L );
			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "name" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( "John Doe" );
		} );
	}

	public interface Property<T> {
		String getName();

		T getValue();
	}

	@Entity( name = "IntegerProperty" )
	public static class IntegerProperty implements Property<Integer> {
		@Id
		private Long id;

		@Column( name = "property_name" )
		private String name;

		@Column( name = "property_value" )
		private Integer value;

		public IntegerProperty() {
		}

		public IntegerProperty(Long id, String name, Integer value) {
			this.id = id;
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Integer getValue() {
			return value;
		}
	}

	@Entity( name = "StringProperty" )
	public static class StringProperty implements Property<String> {
		@Id
		private Long id;

		@Column( name = "property_name" )
		private String name;

		@Column( name = "property_value" )
		private String value;

		public StringProperty() {
		}

		public StringProperty(Long id, String name, String value) {
			this.id = id;
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
	}

	@Entity( name = "PropertyHolder" )
	public static class PropertyHolder {
		@Id
		private Long id;

		@Column( name = "property_type" )
		private String propertyType;

		@Any
		@AnyKeyJavaClass( Long.class )
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValues( {
				@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class ),
				@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		} )
		@Column( name = "property_type", updatable = false, insertable = false )
		@JoinColumn( name = "id", updatable = false, insertable = false )
		@Cascade( CascadeType.ALL )
		private Property property;

		public PropertyHolder() {
		}

		public PropertyHolder(Long id, String propertyType, Property property) {
			this.id = id;
			this.propertyType = propertyType;
			this.property = property;
		}

		public Property getProperty() {
			return property;
		}
	}
}
