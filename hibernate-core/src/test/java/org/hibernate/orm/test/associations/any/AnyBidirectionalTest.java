/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
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
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AnyBidirectionalTest.IntegerProperty.class,
		AnyBidirectionalTest.StringProperty.class,
		AnyBidirectionalTest.PropertyHolder.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16919" )
public class AnyBidirectionalTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new PropertyHolder( 1L, new IntegerProperty( 1L, "acc_num", 1234 ) ) );
			session.persist( new PropertyHolder( 2L, new StringProperty( 2L, "acc_name", "daily" ) ) );
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
	public void testStringProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyHolder propertyHolder = session.find( PropertyHolder.class, 2L );
			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "acc_name" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( "daily" );
		} );
	}

	@Test
	public void testIntegerProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final PropertyHolder propertyHolder = session.find( PropertyHolder.class, 1L );
			assertThat( propertyHolder.getProperty().getName() ).isEqualTo( "acc_num" );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( 1234 );
		} );
	}

	@Test
	public void testInverseAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final StringProperty stringProperty = session.find( StringProperty.class, 2L );
			assertThat( stringProperty.getPropertyHolder().getId() ).isEqualTo( 2L );
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

		@OneToOne
		@JoinColumn( name = "id", insertable = false, updatable = false )
		private PropertyHolder propertyHolder;

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

		public PropertyHolder getPropertyHolder() {
			return propertyHolder;
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

		@OneToOne
		@JoinColumn( name = "id", insertable = false, updatable = false )
		private PropertyHolder propertyHolder;

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

		public PropertyHolder getPropertyHolder() {
			return propertyHolder;
		}
	}

	@Entity( name = "PropertyHolder" )
	public static class PropertyHolder {
		@Id
		private Long id;

		@Any
		@AnyKeyJavaClass( Long.class )
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
		@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		@Column( name = "property_type" )
		@JoinColumn( name = "property_id" )
		@Cascade( CascadeType.ALL )
		private Property property;

		public PropertyHolder() {
		}

		public PropertyHolder(Long id, Property property) {
			this.id = id;
			this.property = property;
		}

		public Long getId() {
			return id;
		}

		public Property getProperty() {
			return property;
		}
	}
}
