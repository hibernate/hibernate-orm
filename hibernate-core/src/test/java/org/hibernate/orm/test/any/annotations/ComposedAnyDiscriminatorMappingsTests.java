/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.annotations.Any;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@DomainModel( annotatedClasses = {
		ComposedAnyDiscriminatorMappingsTests.EntityWithComposedAnyDiscriminatorMappings.class,
		CharProperty.class,
		StringProperty.class,
		IntegerProperty.class
} )
@SessionFactory
public class ComposedAnyDiscriminatorMappingsTests {
	@Test
	public void testUsage(SessionFactoryScope scope) {
		// atm this will blow up because the mapping will fail
		scope.inTransaction( (session) -> {
			session.createQuery( "select s from StringProperty s" ).list();
			session.createQuery( "select ph from PropertyHolder ph" ).list();
		} );
	}

	@Entity( name = "PropertyHolder" )
	@Table( name = "t_any_discrim_composed" )
	public static class EntityWithComposedAnyDiscriminatorMappings {
		@Id
		private Integer id;
		private String name;

		@Any
		@Column(name = "property_type")
		@JoinColumn(name = "property_id")
		@PropertyDiscriminatorMapping
		private Property property;
	}
}
