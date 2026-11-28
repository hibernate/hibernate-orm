package org.hibernate.orm.test.mapping.embeddable;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Whole-aggregate parameters retain the JDBC representation of each HQL occurrence.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {AggregateParameterBindingTest.Holder.class, AggregateParameterBindingTest.SingleHolder.class})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "true"))
@Jira("https://hibernate.atlassian.net/browse/HHH-21270")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class AggregateParameterBindingTest {
	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				final var street = id == 1 ? "Main" : "Other";
				final var holder = new Holder();
				holder.id = id;
				holder.aggregate = address( street, 123 );
				holder.plain = Map.of( "street", street );
				holder.flattened = address( street, 123 );
				holder.details = new Details();
				holder.details.label = "Label";
				holder.details.nested = address( street, 123 );
				session.persist( holder );
			}
			final var single = new SingleHolder();
			single.id = 1;
			single.address = singleAddress( "Main" );
			session.persist( single );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void singleMemberPredicateAndAssignment(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createQuery(
				"from SingleAggregateParameterHolder h where h.address=:value", SingleHolder.class )
				.setParameter( "value", singleAddress( "Main" ) ).getResultList().size() ) );
		scope.inTransaction( session -> assertEquals( 1, session.createMutationQuery(
				"update SingleAggregateParameterHolder h set h.address=:value where h.id=1" )
				.setParameter( "value", singleAddress( "Changed" ) ).executeUpdate() ) );
		scope.inTransaction( session -> assertEquals( "Changed", session.find( SingleHolder.class, 1L ).address.street ) );
	}

	@Test
	void multiMemberPredicates(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( var predicate : List.of( "h.aggregate=:value", ":value=h.aggregate" ) ) {
				final var query = session.createQuery( "from AggregateParameterHolder h where " + predicate, Holder.class );
				assertEquals( 1L, query.setParameter( "value", address( "Main", 123 ) ).getSingleResult().id );
				assertEquals( 2L, query.setParameter( "value", address( "Other", 123 ) ).getSingleResult().id );
				assertEquals( 0, query.setParameter( "value", address( "Main", 456 ) ).getResultList().size() );
			}
		} );
	}

	@Test
	void aggregateListParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery(
					"from AggregateParameterHolder h where h.aggregate in :values", Holder.class );
			assertEquals( 2, query.setParameterList( "values", List.of( address( "Main", 123 ), address( "Other", 123 ) ) )
					.getResultList().size() );
			assertEquals( 0, query.setParameterList( "values", List.of( address( "Missing", 123 ) ) ).getResultList().size() );
		} );
	}

	@Test
	void nullableParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery(
					"from AggregateParameterHolder h where :value is null or h.aggregate=:value", Holder.class );
			assertEquals( 2, query.setParameter( "value", null ).getResultList().size() );
			assertEquals( 1, query.setParameter( "value", address( "Main", 123 ) ).getResultList().size() );
			assertEquals( 2, query.setParameter( "value", null ).getResultList().size() );
		} );
	}

	@Test
	void nullableJsonMapParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery(
					"from AggregateParameterHolder h where :value is null or h.plain=:value", Holder.class );
			assertEquals( 2, query.setParameter( "value", null ).getResultList().size() );
			assertEquals( 1, query.setParameter( "value", Map.of( "street", "Main" ) ).getResultList().size() );
			assertEquals( 0, query.setParameter( "value", Map.of( "street", "Missing" ) ).getResultList().size() );
		} );
	}

	@Test
	void positionalParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1L, session.createQuery(
				"from AggregateParameterHolder h where h.aggregate=?1", Holder.class )
				.setParameter( 1, address( "Main", 123 ) ).getSingleResult().id ) );
	}

	@Test
	void criteriaParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Holder.class );
			final var root = criteria.from( Holder.class );
			final var parameter = builder.parameter( Address.class, "value" );
			criteria.select( root ).where( builder.equal( root.get( "aggregate" ), parameter ) );
			assertEquals( 1L, session.createQuery( criteria ).setParameter( parameter, address( "Main", 123 ) ).getSingleResult().id );
		} );
	}

	@Test
	void reusedCriteriaParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			for ( boolean aggregateFirst : List.of( true, false ) ) {
				final var criteria = builder.createQuery( Holder.class );
				final var root = criteria.from( Holder.class );
				final var parameter = builder.parameter( Address.class, "value" );
				final var aggregate = builder.equal( root.get( "aggregate" ), parameter );
				final var flattened = builder.equal( root.get( "flattened" ), parameter );
				criteria.select( root ).where( aggregateFirst
						? builder.and( aggregate, flattened ) : builder.and( flattened, aggregate ) );
				assertEquals( 1L, session.createQuery( criteria ).setParameter( parameter, address( "Main", 123 ) ).getSingleResult().id );
			}
		} );
	}

	@Test
	void reusedAggregateAndFlattenedParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( var predicate : List.of(
					"h.aggregate=:value and h.flattened=:value",
					"h.flattened=:value and h.aggregate=:value" ) ) {
				final var query = session.createQuery( "from AggregateParameterHolder h where " + predicate, Holder.class );
				assertEquals( 1L, query.setParameter( "value", address( "Main", 123 ) ).getSingleResult().id );
				assertEquals( 2L, query.setParameter( "value", address( "Other", 123 ) ).getSingleResult().id );
				assertEquals( 0, query.setParameter( "value", address( "Main", 456 ) ).getResultList().size() );
			}
		} );
	}

	@Test
	void reusedDifferentAggregateMappings(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( var predicate : List.of(
					"h.aggregate=:value and h.details.nested=:value",
					"h.details.nested=:value and h.aggregate=:value" ) ) {
				assertEquals( 1L, session.createQuery( "from AggregateParameterHolder h where " + predicate, Holder.class )
						.setParameter( "value", address( "Main", 123 ) ).getSingleResult().id );
			}
		} );
	}

	@Test
	void bulkAggregateAssignment(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createMutationQuery(
				"update AggregateParameterHolder h set h.aggregate=:value where h.id=1" )
				.setParameter( "value", address( "Changed", 456 ) ).executeUpdate() ) );
		scope.inTransaction( session -> {
			final var holder = session.find( Holder.class, 1L );
			assertEquals( "Changed", holder.aggregate.street );
			assertEquals( 456, holder.aggregate.zip );
			assertEquals( "Main", holder.flattened.street );
		} );
	}

	@Test
	void bulkNullAssignment(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createMutationQuery(
				"update AggregateParameterHolder h set h.aggregate=:value where h.id=1" )
				.setParameter( "value", null ).executeUpdate() ) );
		scope.inTransaction( session -> assertNull( session.find( Holder.class, 1L ).aggregate ) );
	}

	@Test
	void bulkReusedParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createMutationQuery(
				"update AggregateParameterHolder h set h.aggregate=:value where h.flattened=:value" )
				.setParameter( "value", address( "Main", 123 ) ).executeUpdate() ) );
		scope.inTransaction( session -> assertEquals( "Main", session.find( Holder.class, 1L ).aggregate.street ) );
	}

	@Test
	void nestedAggregateAssignment(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createMutationQuery(
				"update AggregateParameterHolder h set h.details.nested=:value where h.id=1" )
				.setParameter( "value", address( "Changed", 456 ) ).executeUpdate() ) );
		scope.inTransaction( session -> {
			final var details = session.find( Holder.class, 1L ).details;
			assertEquals( "Label", details.label );
			assertEquals( "Changed", details.nested.street );
			assertEquals( 456, details.nested.zip );
		} );
	}

	private static Address address(String street, int zip) {
		final var address = new Address();
		address.street = street;
		address.zip = zip;
		return address;
	}

	private static SingleAddress singleAddress(String street) {
		final var address = new SingleAddress();
		address.street = street;
		return address;
	}

	@Entity(name = "AggregateParameterHolder")
	@Table(name = "aggregate_parameter_holder")
	static class Holder {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Address aggregate;
		@JdbcTypeCode(SqlTypes.JSON)
		Map<String, String> plain;
		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "street", column = @Column(name = "flat_street")),
				@AttributeOverride(name = "zip", column = @Column(name = "flat_zip"))
		})
		Address flattened;
		@Embedded
		Details details;
	}

	@Embeddable
	static class Details {
		String label;
		@JdbcTypeCode(SqlTypes.JSON)
		@AttributeOverride(name = "street", column = @Column(name = "road"))
		Address nested;
	}

	@Embeddable
	static class Address {
		String street;
		int zip;
	}

	@Entity(name = "SingleAggregateParameterHolder")
	@Table(name = "single_aggregate_parameter_holder")
	static class SingleHolder {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		SingleAddress address;
	}

	@Embeddable
	static class SingleAddress {
		String street;
	}
}
