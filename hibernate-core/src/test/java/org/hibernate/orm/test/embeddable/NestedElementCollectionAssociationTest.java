package org.hibernate.orm.test.embeddable;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/// Maps a many-to-one inside a nested embeddable in an element collection.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		NestedElementCollectionAssociationTest.Owner.class,
		NestedElementCollectionAssociationTest.Target.class
})
@SessionFactory
@JiraKey("HHH-8034")
class NestedElementCollectionAssociationTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testCollectionTableMapping(SessionFactoryScope scope) {
		var collection = scope.getMetadataImplementor().getCollectionBinding( Owner.class.getName() + ".elements" );
		var table = collection.getCollectionTable();
		assertThat( table.getName() ).isEqualTo( "hhh8034_elements" );
		assertThat( table.getColumns() ).extracting( org.hibernate.mapping.Column::getName )
				.containsExactlyInAnyOrder( "owner_id", "outer_text", "inner_text", "target_id" );
		assertThat( table.getForeignKeys() )
				.extracting( foreignKey -> foreignKey.getReferencedTable().getName() )
				.containsExactlyInAnyOrder( "hhh8034_owner", "hhh8034_target" );
	}

	@Test
	void testPersistenceRoundTrip(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var first = new Target();
			first.id = 1L;
			first.name = "first target";
			var second = new Target();
			second.id = 2L;
			second.name = "second target";
			session.persist( first );
			session.persist( second );
			var owner = new Owner();
			owner.id = 3L;
			owner.elements.add( element( "first outer", "first inner", first ) );
			owner.elements.add( element( "second outer", "second inner", second ) );
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			var owner = session.find( Owner.class, 3L );
			assertThat( owner.elements ).extracting(
					element -> element.text,
					element -> element.nested.text,
					element -> element.nested.target.id,
					element -> element.nested.target.name
			).containsExactlyInAnyOrder(
					tuple( "first outer", "first inner", 1L, "first target" ),
					tuple( "second outer", "second inner", 2L, "second target" )
			);
			assertThat( owner.elements ).allSatisfy( element ->
					assertThat( element.nested.target ).isSameAs( session.find( Target.class, element.nested.target.id ) ) );
		} );
	}

	private static Element element(String outerText, String innerText, Target target) {
		var element = new Element();
		element.text = outerText;
		element.nested = new Nested();
		element.nested.text = innerText;
		element.nested.target = target;
		return element;
	}

	@Entity(name = "NestedCollectionOwner")
	@Table(name = "hhh8034_owner")
	public static class Owner {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable(name = "hhh8034_elements", joinColumns = @JoinColumn(name = "owner_id"))
		List<Element> elements = new ArrayList<>();
	}

	@Embeddable
	public static class Element {
		@Column(name = "outer_text")
		String text;

		@Embedded
		Nested nested;
	}

	@Embeddable
	public static class Nested {
		@Column(name = "inner_text")
		String text;

		@ManyToOne
		@JoinColumn(name = "target_id")
		Target target;
	}

	@Entity(name = "NestedCollectionTarget")
	@Table(name = "hhh8034_target")
	public static class Target {
		@Id
		Long id;

		String name;
	}
}
