package org.hibernate.temporal.audit.collection;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Audited;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that collection audit tables follow the {@link Audited.Table @Audited.Table}
 * of the owning entity, except for its name.
 */
@DomainModel(annotatedClasses = {
		AuditCustomTableCollectionTest.Owner.class,
		AuditCustomTableCollectionTest.Child.class,
		AuditCustomTableCollectionTest.Doc.class,
		AuditCustomTableCollectionTest.SubDoc.class,
		AuditCustomTableCollectionTest.Vehicle.class,
		AuditCustomTableCollectionTest.Car.class,
		AuditCustomTableCollectionTest.Truck.class,
		AuditCustomTableCollectionTest.Note.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20641")
class AuditCustomTableCollectionTest {
	@Test
	void testElementCollectionAuditTable(DomainModelScope scope) {
		assertDistinctAuditTable( scope, "tags" );
	}

	@Test
	void testOneToManyJoinColumnAuditTable(DomainModelScope scope) {
		assertDistinctAuditTable( scope, "children" );
	}

	@Test
	void testInheritedCollectionsUseOwnerAuditTable(DomainModelScope scope) {
		assertOwnerAuditTable( scope, Doc.class.getName() + ".baseTags" );
		assertOwnerAuditTable( scope, SubDoc.class.getName() + ".subTags" );
		assertOwnerAuditTable( scope, SubDoc.class.getName() + ".subChildren" );
	}

	@Test
	void testOverrideMergesCollectionTableMembers(DomainModelScope scope) {
		final var table = scope.getDomainModel()
				.getCollectionBinding( Doc.class.getName() + ".renamedTags" ).getAuxiliaryTable();
		assertThat( table.getSchema() ).isEqualTo( "override_schema" );
		assertThat( table.getName() ).isEqualTo( "renamed_tags_hist" );
	}

	@Test
	void testJoinedSubclassAuditTables(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		final var rootAuditTable = metadata.getEntityBinding( Vehicle.class.getName() ).getAuxiliaryTable();
		assertThat( rootAuditTable.getName() ).isEqualTo( "vehicle_history" );
		// The root's audit table name never applies to a subclass audit table
		assertThat( metadata.getEntityBinding( Truck.class.getName() ).getAuxiliaryTable() )
				.isNotSameAs( rootAuditTable );
		assertThat( metadata.getCollectionBinding( Car.class.getName() + ".features" ).getAuxiliaryTable().getSchema() )
				.isEqualTo( "car_audit" );
	}

	@Test
	void testOverridesOnUnauditedEntity(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		assertThat( metadata.getCollectionBinding( Note.class.getName() + ".auditedTags" ).getAuxiliaryTable() )
				.isNull();
		assertThat( metadata.getCollectionBinding( Note.class.getName() + ".plainTags" ).getAuxiliaryTable().getName() )
				.isEqualTo( "plain_tags_hist" );
	}

	private static void assertDistinctAuditTable(DomainModelScope scope, String role) {
		final var metadata = scope.getDomainModel();
		final var entityAuditTable = metadata.getEntityBinding( Owner.class.getName() ).getAuxiliaryTable();
		final var collectionAuditTable =
				metadata.getCollectionBinding( Owner.class.getName() + "." + role ).getAuxiliaryTable();
		assertThat( entityAuditTable.getName() ).isEqualTo( "owner_history" );
		assertThat( collectionAuditTable ).isNotNull();
		assertThat( collectionAuditTable )
				.as( "Audit table of collection '%s' (columns %s)", role, collectionAuditTable.getColumns() )
				.isNotSameAs( entityAuditTable );
		assertThat( collectionAuditTable.getName() ).isNotEqualToIgnoringCase( entityAuditTable.getName() );
	}

	private static void assertOwnerAuditTable(DomainModelScope scope, String role) {
		final var table = scope.getDomainModel().getCollectionBinding( role ).getAuxiliaryTable();
		assertThat( table.getSchema() ).as( role ).isEqualTo( "audit" );
		assertThat( table.getColumn( new Column( "REV_ID" ) ) ).as( role ).isNotNull();
		assertThat( table.getColumn( new Column( "REV_KIND" ) ) ).as( role ).isNotNull();
	}

	@Audited
	@Audited.Table(name = "owner_history")
	@Entity(name = "Owner")
	static class Owner {
		@Id
		long id;
		String name;
		@ElementCollection
		Set<String> tags = new HashSet<>();
		@OneToMany
		@JoinColumn(name = "owner_id")
		Set<Child> children = new HashSet<>();
	}

	@Audited
	@Entity(name = "Child")
	static class Child {
		@Id
		long id;
		String name;
	}

	@MappedSuperclass
	static class Base {
		@ElementCollection
		Set<String> baseTags = new HashSet<>();
	}

	@Audited
	@Audited.Table(schema = "audit", changesetIdColumn = "REV_ID", modificationTypeColumn = "REV_KIND")
	@Audited.Override(
			name = "renamedTags",
			collectionTable = @Audited.CollectionTable(schema = "override_schema")
	)
	@Entity(name = "Doc")
	static class Doc extends Base {
		@Id
		long id;
		@ElementCollection
		@Audited.CollectionTable(name = "renamed_tags_hist", schema = "member_schema")
		Set<String> renamedTags = new HashSet<>();
	}

	@Audited
	@Audited.Table(name = "vehicle_history")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Entity(name = "Vehicle")
	static class Vehicle {
		@Id
		long id;
	}

	@Audited.Table(schema = "car_audit")
	@Entity(name = "Car")
	static class Car extends Vehicle {
		@ElementCollection
		Set<String> features = new HashSet<>();
	}

	@Entity(name = "Truck")
	static class Truck extends Vehicle {
		int axles;
	}

	@Entity(name = "SubDoc")
	static class SubDoc extends Doc {
		@ElementCollection
		Set<String> subTags = new HashSet<>();
		@OneToMany
		@JoinColumn(name = "doc_id")
		Set<Child> subChildren = new HashSet<>();
	}

	@MappedSuperclass
	static class NoteBase {
		@Audited
		@ElementCollection
		Set<String> auditedTags = new HashSet<>();
		@ElementCollection
		Set<String> plainTags = new HashSet<>();
	}

	@Audited.Override(name = "auditedTags", isAudited = false)
	@Audited.Override(name = "plainTags", collectionTable = @Audited.CollectionTable(name = "plain_tags_hist"))
	@Entity(name = "Note")
	static class Note extends NoteBase {
		@Id
		long id;
	}
}
