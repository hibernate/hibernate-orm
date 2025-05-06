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
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.CascadeType.ALL;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		IntegerProperty.class,
		StringProperty.class,
		ManyToAnyMergeTest.PropertyHolder.class,
		PropertyRepository.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16532" )
public class ManyToAnyMergeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IntegerProperty ageProperty = new IntegerProperty();
			ageProperty.setId( 1L );
			ageProperty.setName( "age" );
			ageProperty.setValue( 23 );
			session.persist( ageProperty );
			final StringProperty nameProperty = new StringProperty();
			nameProperty.setId( 2L );
			nameProperty.setName( "name" );
			nameProperty.setValue( "John Doe" );
			session.persist( nameProperty );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PropertyRepository" ).executeUpdate();
			session.createMutationQuery( "delete from PropertyHolder" ).executeUpdate();
			session.createMutationQuery( "delete from IntegerProperty" ).executeUpdate();
			session.createMutationQuery( "delete from StringProperty" ).executeUpdate();
		} );
	}

	@Test
	public void testAnyMerge(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final StringProperty nameProperty = session.find( StringProperty.class, 2L );
			final PropertyHolder propertyHolder = new PropertyHolder();
			propertyHolder.setId( 3L );
			propertyHolder.setProperty( nameProperty );
			session.persist( propertyHolder );
			session.flush();
			session.clear();
			( (StringProperty) propertyHolder.getProperty() ).setValue( "Updated Name" );
			session.merge( propertyHolder );
		} );
		scope.inTransaction( session -> {
			final PropertyHolder propertyHolder = session.find( PropertyHolder.class, 3L );
			assertThat( propertyHolder.getProperty().getValue() ).isEqualTo( "Updated Name" );
		} );
	}

	@Test
	public void testManyToAnyMerge(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IntegerProperty ageProperty = session.find( IntegerProperty.class, 1L );
			final StringProperty nameProperty = session.find( StringProperty.class, 2L );
			final PropertyRepository propertyRepository = new PropertyRepository();
			propertyRepository.setId( 4L );
			session.persist( propertyRepository );
			session.flush();
			session.clear();
			assertThat( propertyRepository.getProperties() ).hasSize( 0 );
			ageProperty.setName( "updated_" + ageProperty.getName() );
			propertyRepository.getProperties().add( ageProperty );
			nameProperty.setName( "updated_" + nameProperty.getName() );
			propertyRepository.getProperties().add( nameProperty );
			session.merge( propertyRepository );
		} );
		scope.inTransaction( session -> {
			final PropertyRepository propertyRepository = session.find( PropertyRepository.class, 4L );
			assertThat( propertyRepository.getProperties() ).hasSize( 2 );
			for ( Property<?> property : propertyRepository.getProperties() ) {
				assertThat( property.getValue() ).isNotNull();
				assertThat( property.getName() ).startsWith( "updated_" );
			}
		} );
	}

	@Entity( name = "PropertyHolder" )
	@Table( name = "property_holder" )
	public static class PropertyHolder {
		@Id
		private Long id;

		@Any
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
		@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
		@AnyKeyJavaClass( Long.class )
		@Column( name = "property_type" )
		@JoinColumn( name = "property_id" )
		@Cascade( ALL )
		private Property property;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Property getProperty() {
			return property;
		}

		public void setProperty(Property property) {
			this.property = property;
		}
	}
}
