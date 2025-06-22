/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		AnyNullabilityTest.IntegerProperty.class,
		AnyNullabilityTest.StringProperty.class,
		AnyNullabilityTest.OptionalPropertyHolder.class,
		AnyNullabilityTest.NonOptionalPropertyHolder.class,
		AnyNullabilityTest.NonNullablePropertyHolder.class,
} )
@SessionFactory
public class AnyNullabilityTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from OptionalPropertyHolder" ).executeUpdate();
		} );
	}

	@Test
	public void testOptionalAny(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final OptionalPropertyHolder emptyPropertyHolder = new OptionalPropertyHolder();
			emptyPropertyHolder.setId( 1L );
			session.persist( emptyPropertyHolder );
		} );
		scope.inTransaction( session -> {
			final OptionalPropertyHolder propertyHolder = session.find( OptionalPropertyHolder.class, 1L );
			assertThat( propertyHolder.getProperty() ).isNull();
		} );
	}

	@Test
	public void testNonOptionalAny(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final NonOptionalPropertyHolder emptyPropertyHolder = new NonOptionalPropertyHolder();
				emptyPropertyHolder.setId( 2L );
				session.persist( emptyPropertyHolder );
			} );
			fail( "Non-optional any was persisted with a non-valid `null` value" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( PropertyValueException.class );
			assertThat( e.getMessage() ).contains( "not-null property references a null or transient value" );
		}
	}

	@Test
	public void testOptionalAnyNonNullable(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final NonNullablePropertyHolder emptyPropertyHolder = new NonNullablePropertyHolder();
				emptyPropertyHolder.setId( 3L );
				session.persist( emptyPropertyHolder );
			} );
			fail( "Non-optional any was persisted with a non-valid `null` value" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( PropertyValueException.class );
			assertThat( e.getMessage() ).contains( "not-null property references a null or transient value" );
		}
	}

	public interface Property<T> {
		String getName();

		T getValue();
	}

	@Entity( name = "IntegerProperty" )
	public static class IntegerProperty implements Property<Integer> {
		@Id
		private Long id;

		private String name;

		private Integer value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity( name = "StringProperty" )
	public static class StringProperty implements Property<String> {
		@Id
		private Long id;

		private String name;

		private String value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity( name = "OptionalPropertyHolder" )
	public static class OptionalPropertyHolder {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Property<?> getProperty() {
			return property;
		}

		public void setProperty(Property<?> property) {
			this.property = property;
		}

		@Any
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
		@AnyKeyJavaClass( Long.class )
		@Column( name = "property_type" )
		@JoinColumn( name = "property_id" )
		private Property<?> property;
	}

	@Entity( name = "NonOptionalPropertyHolder" )
	public static class NonOptionalPropertyHolder {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Property<?> getProperty() {
			return property;
		}

		public void setProperty(Property<?> property) {
			this.property = property;
		}

		@Any( optional = false )
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
		@AnyKeyJavaClass( Long.class )
		@Column( name = "property_type" )
		@JoinColumn( name = "property_id" )
		private Property<?> property;
	}

	@Entity( name = "NonNullablePropertyHolder" )
	public static class NonNullablePropertyHolder {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Property<?> getProperty() {
			return property;
		}

		public void setProperty(Property<?> property) {
			this.property = property;
		}

		@Any( optional = true )
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
		@AnyKeyJavaClass( Long.class )
		@Column( name = "property_type" )
		@JoinColumn( name = "property_id", nullable = false )
		private Property<?> property;
	}
}
