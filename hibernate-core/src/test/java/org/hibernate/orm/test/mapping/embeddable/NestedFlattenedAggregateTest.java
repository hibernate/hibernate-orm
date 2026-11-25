package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Aggregate mutations and JDBC value indices through ordinary embeddables.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		NestedFlattenedAggregateTest.Holder.class,
		NestedFlattenedAggregateTest.DeepHolder.class,
		NestedFlattenedAggregateTest.PolymorphicHolder.class,
		NestedFlattenedAggregateTest.PolymorphicDetails.class,
		NestedFlattenedAggregateTest.ExtendedDetails.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20913")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class NestedFlattenedAggregateTest {
	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var holder = new Holder();
			holder.id = 10;
			holder.outer = value();
			session.persist( holder );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void nestedJdbcValueIndices(SessionFactoryScope scope) {
		final var entity = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( DeepHolder.class );
		final var mapping = ( (EmbeddableValuedModelPart) entity.findAttributeMapping( "envelope" ) ).getEmbeddableTypeDescriptor();
		final int index = mapping.getSelectableIndex( "zz" );
		assertEquals( 5, mapping.getJdbcValueCount() );
		assertEquals( 4, index );
		assertEquals( "zz", mapping.getJdbcValueSelectable( index ).getSelectionExpression() );
	}

	@Test
	void nestedPolymorphicValues(SessionFactoryScope scope) {
		final var entity = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( PolymorphicHolder.class );
		final var mapping = ( (EmbeddableValuedModelPart) entity.findAttributeMapping( "envelope" ) ).getEmbeddableTypeDescriptor();
		assertEquals( 5, mapping.getJdbcValueCount() );
		assertEquals( 4, mapping.getSelectableIndex( "tail" ) );
		assertEquals( "tail", mapping.getJdbcValueSelectable( 4 ).getSelectionExpression() );
		scope.inTransaction( session -> {
			final var details = new ExtendedDetails();
			details.address = value().details.first;
			details.text = "Text";
			details.extra = "Extra";
			final var holder = new PolymorphicHolder();
			holder.id = 1;
			holder.envelope = new PolymorphicEnvelope();
			holder.envelope.details = details;
			holder.envelope.tail = "Tail";
			session.persist( holder );
		} );
		scope.inTransaction( session -> {
			final var holder = session.find( PolymorphicHolder.class, 1L );
			final var details = assertInstanceOf( ExtendedDetails.class, holder.envelope.details );
			assertEquals( "Extra", details.extra );
			assertEquals( "Text", details.text );
			assertEquals( "Main", details.address.street );
			assertEquals( "Tail", holder.envelope.tail );
			details.address.zip = 789;
			holder.envelope.tail = "Changed";
		} );
		scope.inTransaction( session -> {
			final var holder = session.find( PolymorphicHolder.class, 1L );
			assertEquals( 789, holder.envelope.details.address.zip );
			assertEquals( "Changed", holder.envelope.tail );
		} );
	}

	@Test
	void persistAndUpdateDeepEmbedding(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var holder = new DeepHolder();
			holder.id = 1;
			holder.envelope = new Envelope();
			holder.envelope.details = value().details;
			holder.envelope.zz = "Tail";
			session.persist( holder );
		} );
		scope.inTransaction( session -> {
			final var holder = session.find( DeepHolder.class, 1L );
			assertEquals( "Tail", holder.envelope.zz );
			assertEquals( 456, holder.envelope.details.second.zip );
			holder.envelope.zz = "Changed";
			holder.envelope.details.second.zip = 789;
		} );
		scope.inTransaction( session -> {
			final var holder = session.find( DeepHolder.class, 1L );
			assertEquals( "Changed", holder.envelope.zz );
			assertEquals( 789, holder.envelope.details.second.zip );
			assertEquals( 123, holder.envelope.details.first.zip );
		} );
	}

	@Test
	void persist(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var holder = new Holder();
			holder.id = 1;
			holder.outer = value();
			session.persist( holder );
		} );
		scope.inTransaction( session -> check( session.find( Holder.class, 1L ).outer ) );
	}

	@Test
	void loadSeeded(SessionFactoryScope scope) {
		scope.inTransaction( session -> check( session.find( Holder.class, 10L ).outer ) );
	}

	@Test
	void selectOuter(SessionFactoryScope scope) {
		scope.inTransaction( session -> check( session.createQuery(
				"select h.outer from AggregateShapeHolder h where h.id=10", Outer.class ).getSingleResult() ) );
	}

	@Test
	void selectMembers(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var row = session.createQuery(
					"select h.outer.details.a, h.outer.details.first.street, h.outer.details.first.zip, "
							+ "h.outer.details.second.street, h.outer.details.second.zip, h.outer.details.z "
							+ "from AggregateShapeHolder h where h.id=10", Object[].class ).getSingleResult();
			assertEquals( "A", row[0] );
			assertEquals( "Main", row[1] );
			assertEquals( 123, row[2] );
			assertEquals( "Second", row[3] );
			assertEquals( 456, row[4] );
			assertEquals( "Z", row[5] );
		} );
	}

	@Test
	void predicateOuter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery( "from AggregateShapeHolder h where h.outer=:value", Holder.class );
			assertEquals( 1, query.setParameter( "value", value() ).getResultList().size() );
			final var different = value();
			different.details.second.zip = 999;
			assertEquals( 0, query.setParameter( "value", different ).getResultList().size() );
		} );
	}

	@Test
	void mutateMember(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Holder.class, 10L ).outer.details.first.zip = 789 );
		scope.inTransaction( session -> assertEquals( 789, session.find( Holder.class, 10L ).outer.details.first.zip ) );
	}

	private static Outer value() {
		final var outer = new Outer();
		outer.details = new Details();
		outer.details.a = "A";
		outer.details.z = "Z";
		outer.details.first = new Address();
		outer.details.first.street = "Main";
		outer.details.first.zip = 123;
		outer.details.second = new Address();
		outer.details.second.street = "Second";
		outer.details.second.zip = 456;
		return outer;
	}

	private static void check(Outer outer) {
		assertEquals( "A", outer.details.a );
		assertEquals( "Main", outer.details.first.street );
		assertEquals( 123, outer.details.first.zip );
		assertEquals( "Second", outer.details.second.street );
		assertEquals( 456, outer.details.second.zip );
		assertEquals( "Z", outer.details.z );
	}

	@Entity(name = "AggregateShapeHolder")
	@Table(name = "aggregate_shape_probe")
	static class Holder {
		@Id
		long id;
		@Embedded
		Outer outer;
	}

	@Entity(name = "DeepAggregateShapeHolder")
	@Table(name = "deep_aggregate_shape_probe")
	static class DeepHolder {
		@Id
		long id;
		@Embedded
		Envelope envelope;
	}

	@Entity(name = "PolymorphicAggregateShapeHolder")
	@Table(name = "polymorphic_aggregate_shape_test")
	static class PolymorphicHolder {
		@Id
		long id;
		@Embedded
		PolymorphicEnvelope envelope;
	}

	@Embeddable
	static class PolymorphicEnvelope {
		@Embedded
		PolymorphicDetails details;
		String tail;
	}

	@Embeddable
	@DiscriminatorColumn(name = "details_type")
	@DiscriminatorValue("base")
	static class PolymorphicDetails {
		@JdbcTypeCode(SqlTypes.JSON)
		Address address;
		String text;
	}

	@Embeddable
	@DiscriminatorValue("extended")
	static class ExtendedDetails extends PolymorphicDetails {
		String extra;
	}

	@Embeddable
	static class Envelope {
		@Embedded
		Details details;
		String zz;
	}

	@Embeddable
	static class Outer {
		@Embedded
		Details details;
	}

	@Embeddable
	static class Details {
		String a;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "first_json")
		Address first;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "second_json")
		Address second;
		String z;
	}

	@Embeddable
	static class Address {
		String street;
		int zip;
	}
}
