package org.hibernate.orm.test.mapping.embeddable;

import java.util.ArrayList;
import java.util.List;

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

/// Aggregate column write flags apply through an enclosing ordinary embeddable.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FlattenedEmbeddableAggregateFlagsTest.Holder.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20913")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class FlattenedEmbeddableAggregateFlagsTest {
	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var holder = new Holder();
			holder.id = 1;
			holder.details = new Details();
			holder.details.label = "Original";
			holder.details.ignored = address( "Ignored" );
			holder.details.insertOnly = address( "Main" );
			holder.details.updateOnly = address( "Update" );
			session.persist( holder );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void mutationColumnOffsets(SessionFactoryScope scope) {
		final var entity = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Holder.class );
		final var details = (EmbeddableValuedModelPart) entity.findAttributeMapping( "details" );
		final var inserts = new ArrayList<String>();
		final var updates = new ArrayList<String>();
		details.getEmbeddableTypeDescriptor().forEachInsertable( 3,
				(index, selectable) -> inserts.add( index + ":" + selectable.getSelectionExpression() ) );
		details.getEmbeddableTypeDescriptor().forEachUpdatable( 3,
				(index, selectable) -> updates.add( index + ":" + selectable.getSelectionExpression() ) );
		assertEquals( List.of( "4:insert_json", "5:label" ), inserts );
		assertEquals( List.of( "5:label", "6:update_json" ), updates );
	}

	@Test
	void insertFlags(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var details = session.find( Holder.class, 1L ).details;
			assertEquals( "Original", details.label );
			assertNull( details.ignored );
			assertEquals( "Main", details.insertOnly.street );
			assertNull( details.updateOnly );
		} );
	}

	@Test
	void updateFlags(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update aggregate_flags_test set ignored_json=insert_json,update_json=insert_json where id=1" )
				.executeUpdate() );
		scope.inTransaction( session -> {
			final var details = session.find( Holder.class, 1L ).details;
			assertEquals( "Main", details.ignored.street );
			assertEquals( "Main", details.updateOnly.street );
			details.label = "Changed";
			details.ignored.street = "Ignored change";
			details.insertOnly.street = "Insert change";
			details.updateOnly.street = "Update change";
		} );
		scope.inTransaction( session -> {
			final var details = session.find( Holder.class, 1L ).details;
			assertEquals( "Changed", details.label );
			assertEquals( "Main", details.ignored.street );
			assertEquals( "Main", details.insertOnly.street );
			assertEquals( "Update change", details.updateOnly.street );
		} );
	}

	private static Address address(String street) {
		final var address = new Address();
		address.street = street;
		return address;
	}

	@Entity(name = "AggregateFlagsHolder")
	@Table(name = "aggregate_flags_test")
	static class Holder {
		@Id
		long id;
		@Embedded
		Details details;
	}

	@Embeddable
	static class Details {
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "ignored_json", insertable = false, updatable = false)
		Address ignored;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "insert_json", updatable = false)
		Address insertOnly;
		String label;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "update_json", insertable = false)
		Address updateOnly;
	}

	@Embeddable
	static class Address {
		String street;
	}
}
