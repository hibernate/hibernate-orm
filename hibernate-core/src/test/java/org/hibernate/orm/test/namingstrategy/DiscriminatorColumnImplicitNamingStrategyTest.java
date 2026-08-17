/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;


import java.io.Serializable;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitDiscriminatorColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.EntityMappingType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy#determineDiscriminatorColumnName}
 * is consulted when no explicit discriminator column name is given via {@link DiscriminatorColumn#name()}.
 */
@JiraKey("HHH-20613")
@ServiceRegistry(settings = {
		@Setting(
				name = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				value = "org.hibernate.orm.test.namingstrategy.DiscriminatorColumnImplicitNamingStrategyTest$CustomNamingStrategy"
		)
})
@DomainModel(annotatedClasses = {
		DiscriminatorColumnImplicitNamingStrategyTest.Vehicle.class,
		DiscriminatorColumnImplicitNamingStrategyTest.Car.class,
		DiscriminatorColumnImplicitNamingStrategyTest.Truck.class,
		DiscriminatorColumnImplicitNamingStrategyTest.ExplicitRoot.class,
		DiscriminatorColumnImplicitNamingStrategyTest.ExplicitChild.class
})
@SessionFactory
public class DiscriminatorColumnImplicitNamingStrategyTest {

	@Test
	public void testImplicitNameComesFromNamingStrategy(SessionFactoryScope scope) {
		final EntityMappingType carMapping = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( Car.class );
		assertEquals( "vehicle_type", carMapping.getDiscriminatorMapping().getSelectionExpression(),
				"Discriminator column name should come from the custom ImplicitNamingStrategy" );
	}

	@Test
	public void testExplicitNameOverridesNamingStrategy(SessionFactoryScope scope) {
		final EntityMappingType explicitChildMapping = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( ExplicitChild.class );
		assertEquals( "explicit_disc", explicitChildMapping.getDiscriminatorMapping().getSelectionExpression(),
				"Explicit @DiscriminatorColumn name must take precedence over the ImplicitNamingStrategy" );
	}

	@Entity(name = "Vehicle")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class Vehicle {
		@Id
		public Integer id;

		protected Vehicle() {
		}

		protected Vehicle(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Car")
	public static class Car extends Vehicle {
		public Car() {
		}

		public Car(Integer id) {
			super( id );
		}
	}

	@Entity(name = "Truck")
	public static class Truck extends Vehicle {
		public Truck() {
		}

		public Truck(Integer id) {
			super( id );
		}
	}

	@Entity(name = "ExplicitRoot")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "explicit_disc")
	public static abstract class ExplicitRoot {
		@Id
		public Integer id;

		protected ExplicitRoot() {
		}
	}

	@Entity(name = "ExplicitChild")
	public static class ExplicitChild extends ExplicitRoot {
		public ExplicitChild() {
		}
	}

	public static class CustomNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl implements Serializable {
		@Override
		public Identifier determineDiscriminatorColumnName(ImplicitDiscriminatorColumnNameSource source) {
			final EntityNaming entityNaming = source.getEntityNaming();
			final MetadataBuildingContext context = source.getBuildingContext();
			// Use the entity's JPA name (lower-cased) plus "_type" as the discriminator column name
			final String jpaName = entityNaming.getJpaEntityName();
			final String columnName = ( jpaName != null && !jpaName.isBlank() ? jpaName : entityNaming.getEntityName() )
											.toLowerCase() + "_type";
			return toIdentifier( columnName, context );
		}
	}
}
