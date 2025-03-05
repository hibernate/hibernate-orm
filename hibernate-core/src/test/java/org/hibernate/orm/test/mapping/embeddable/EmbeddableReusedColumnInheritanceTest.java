/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author gtoison
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EmbeddableReusedColumnInheritanceTest.Food.class,
		EmbeddableReusedColumnInheritanceTest.SolidFood.class,
		EmbeddableReusedColumnInheritanceTest.LiquidFood.class,
		EmbeddableReusedColumnInheritanceTest.WeightAndVolume.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17156" )
public class EmbeddableReusedColumnInheritanceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SolidFood solid = new SolidFood();
			solid.setId( 1L );
			solid.setWeight( 42.0D );
			session.persist( solid );
			final LiquidFood liquid = new LiquidFood();
			liquid.setId( 2L );
			liquid.setWeightAndVolume( new WeightAndVolume( 1.0D, 2.0D ) );
			session.persist( liquid );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Food" ).executeUpdate() );
	}

	@Test
	public void testSolidFood(SessionFactoryScope scope) {
		scope.inSession( s -> {
			final Food food = s.getReference( Food.class, 1L );
			assertThat( Hibernate.isInitialized( food ) ).isFalse();
			final Object unproxied = Hibernate.unproxy( food );
			assertThat( unproxied ).isInstanceOf( SolidFood.class );
			assertThat( ( (SolidFood) unproxied ).getWeight() ).isEqualTo( 42.0D );
		} );
	}

	@Test
	public void testLiquidFood(SessionFactoryScope scope) {
		scope.inSession( s -> {
			final Food food = s.getReference( Food.class, 2L );
			assertThat( Hibernate.isInitialized( food ) ).isFalse();
			final Object unproxied = Hibernate.unproxy( food );
			assertThat( unproxied ).isInstanceOf( LiquidFood.class );
			assertThat( ( (LiquidFood) unproxied ).getWeightAndVolume().getWeight() ).isEqualTo( 1.0D );
			assertThat( ( (LiquidFood) unproxied ).getWeightAndVolume().getVolume() ).isEqualTo( 2.0D );
		} );
	}

	@Entity( name = "Food" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( discriminatorType = DiscriminatorType.STRING, name = "type" )
	@DiscriminatorValue( value = "FOOD" )
	public static class Food {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "SolidFood" )
	@DiscriminatorValue( "SOLID" )
	public static class SolidFood extends Food {
		private Double weight;

		public Double getWeight() {
			return weight;
		}

		public void setWeight(Double weight) {
			this.weight = weight;
		}
	}

	@Entity( name = "LiquidFood" )
	@DiscriminatorValue( "LIQUID" )
	public static class LiquidFood extends Food {
		@Embedded
		private WeightAndVolume weightAndVolume;

		public WeightAndVolume getWeightAndVolume() {
			return weightAndVolume;
		}

		public void setWeightAndVolume(WeightAndVolume weightAndVolume) {
			this.weightAndVolume = weightAndVolume;
		}
	}

	@Embeddable
	public static class WeightAndVolume {
		private double weight;
		private double volume;

		public WeightAndVolume() {
		}

		public WeightAndVolume(double weight, double volume) {
			this.weight = weight;
			this.volume = volume;
		}

		public double getWeight() {
			return weight;
		}

		public void setWeight(double weight) {
			this.weight = weight;
		}

		public double getVolume() {
			return volume;
		}

		public void setVolume(double volume) {
			this.volume = volume;
		}
	}
}
