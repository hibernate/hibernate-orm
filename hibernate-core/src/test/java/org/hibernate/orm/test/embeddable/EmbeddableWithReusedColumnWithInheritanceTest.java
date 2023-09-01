package org.hibernate.orm.test.embeddable;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Parent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author gtoison
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		EmbeddableWithReusedColumnWithInheritanceTest.Food.class,
		EmbeddableWithReusedColumnWithInheritanceTest.SolidFood.class,
		EmbeddableWithReusedColumnWithInheritanceTest.LiquidFood.class,
		EmbeddableWithReusedColumnWithInheritanceTest.WeightAndVolume.class
})
@JiraKey("HHH-17156")
public class EmbeddableWithReusedColumnWithInheritanceTest {

	@Test
	public void test(SessionFactoryScope scope) {
		SolidFood solid = new SolidFood();

		scope.inTransaction( s -> {
			solid.setWeight( 42d );

			s.persist( solid );
		} );

		scope.inSession( s -> {
			Food food = s.getReference( Food.class, solid.getId() );
			assertFalse( Hibernate.isInitialized( food ) );
			Object unproxied = Hibernate.unproxy( food );
			assertThat( unproxied ).isInstanceOf( SolidFood.class );
		} );
	}

	@Entity(name = "Food")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "FOOD")
	public static class Food {
		Long id;

		public Food() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "SolidFood")
	@DiscriminatorValue("SOLID")
	public static class SolidFood extends Food {
		@Column(name = "weight")
		Double weight;

		public SolidFood() {
		}

		public Double getWeight() {
			return weight;
		}

		public void setWeight(Double weight) {
			this.weight = weight;
		}
	}

	@Entity(name = "LiquidFood")
	@DiscriminatorValue("LIQUID")
	public static class LiquidFood extends Food {
		
		@Embedded
		@AttributeOverrides(value = { 
				@AttributeOverride(name = "weight", column = @Column(name = "weight")),
				@AttributeOverride(name = "volume", column = @Column(name = "volume")) })
		WeightAndVolume weightAndVolume;

		public LiquidFood() {
		}

		public WeightAndVolume getWeightAndVolume() {
			return weightAndVolume;
		}

		public void setWeightAndVolume(WeightAndVolume weightAndVolume) {
			this.weightAndVolume = weightAndVolume;
		}
	}

	@Embeddable
	public static class WeightAndVolume {
		double weight;
		double volume;
		
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