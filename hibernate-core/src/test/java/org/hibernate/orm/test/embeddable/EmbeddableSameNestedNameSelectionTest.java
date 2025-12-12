/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		EmbeddableSameNestedNameSelectionTest.Material.class,
		EmbeddableSameNestedNameSelectionTest.Weight.class,
		EmbeddableSameNestedNameSelectionTest.Length.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19962")
@Jira("https://hibernate.atlassian.net/browse/HHH-19985")
public class EmbeddableSameNestedNameSelectionTest {

	@Test
	void testPlainEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery( "from Material", Material.class )
					.getSingleResult();
			assertThat( result.getWeight().getValue() ).isEqualTo( "10" );
			assertThat( result.getLength().getValue() ).isEqualTo( "2" );
		} );
	}

	@Test
	void testSubqueryEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery(
					"select q.weight as weight, q.length as length from (select m.weight as weight, m.length as length from Material m) q",
					Tuple.class
			).getSingleResult();
			assertEmbeddedTuple( result );
		} );
	}

	@Test
	void testEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery(
					"select m.weight as weight, m.length as length from Material m",
					Tuple.class
			).getSingleResult();
			assertEmbeddedTuple( result );
		} );
	}

	private void assertEmbeddedTuple(Tuple result) {
		final var weight = result.get( "weight", Weight.class );
		assertThat( weight.getValue() ).isEqualTo( "10" );
		assertThat( weight.getUnit() ).isEqualTo( WeightUnit.KILOGRAM );
		final var length = result.get( "length", Length.class );
		assertThat( length.getValue() ).isEqualTo( "2" );
		assertThat( length.getUnit() ).isEqualTo( LengthUnit.METER );
	}

	@Test
	void testSubqueryScalar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery(
					"select q.weight, q.weight_unit, q.length, q.length_unit from "
					+ "(select m.weight.value as weight, m.weight.unit as weight_unit, m.length.value as length, m.length.unit as length_unit from Material m) q",
					Tuple.class
			).getSingleResult();
			assertScalarTuple( result );
		} );
	}

	@Test
	void testScalar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery(
					"select m.weight.value, m.weight.unit, m.length.value, m.length.unit from Material m",
					Tuple.class
			).getSingleResult();
			assertScalarTuple( result );
		} );
	}

	private void assertScalarTuple(Tuple result) {
		assertThat( result.get( 0, String.class ) ).isEqualTo( "10" );
		assertThat( result.get( 1, WeightUnit.class ) ).isEqualTo( WeightUnit.KILOGRAM );
		assertThat( result.get( 2, String.class ) ).isEqualTo( "2" );
		assertThat( result.get( 3, LengthUnit.class ) ).isEqualTo( LengthUnit.METER );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new Material( 1L, new Weight( "10", WeightUnit.KILOGRAM ), new Length( "2", LengthUnit.METER ) )
		) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Material")
	static class Material {
		@Id
		private Long id;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "value", column = @Column(name = "weight_value")),
				@AttributeOverride(name = "unit", column = @Column(name = "weight_unit")),
		})
		private Weight weight;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "value", column = @Column(name = "length_value")),
				@AttributeOverride(name = "unit", column = @Column(name = "length_unit")),
		})
		private Length length;

		public Material() {
		}

		public Material(Long id, Weight weight, Length length) {
			this.id = id;
			this.weight = weight;
			this.length = length;
		}

		public Long getId() {
			return id;
		}

		public Weight getWeight() {
			return weight;
		}

		public void setWeight(Weight weight) {
			this.weight = weight;
		}

		public Length getLength() {
			return length;
		}

		public void setLength(Length length) {
			this.length = length;
		}
	}

	@Embeddable
	static class Weight {
		private String value;

		private WeightUnit unit;

		public Weight() {
		}

		public Weight(String value, WeightUnit unit) {
			this.value = value;
			this.unit = unit;
		}

		public String getValue() {
			return value;
		}

		public WeightUnit getUnit() {
			return unit;
		}
	}

	enum WeightUnit {
		KILOGRAM,
		POUND
	}

	@Embeddable
	static class Length {
		private String value;

		private LengthUnit unit;

		public Length() {
		}

		public Length(String value, LengthUnit unit) {
			this.value = value;
			this.unit = unit;
		}

		public String getValue() {
			return value;
		}

		public LengthUnit getUnit() {
			return unit;
		}
	}

	enum LengthUnit {
		METER,
		FOOT
	}
}
