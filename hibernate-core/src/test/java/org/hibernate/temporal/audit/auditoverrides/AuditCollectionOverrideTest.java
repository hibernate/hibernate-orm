package org.hibernate.temporal.audit.auditoverrides;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.temporal.audit.auditoverrides.AuditOverrideAssertions.assertNoTable;
import static org.hibernate.temporal.audit.auditoverrides.AuditOverrideAssertions.assertTable;

@AuditedTest
@DomainModel(annotatedClasses = {
		AuditCollectionOverrideTest.Owner.class,
		AuditCollectionOverrideTest.Child.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.auditoverrides.AuditCollectionOverrideTest$TxIdSupplier"
))
@Jira("https://hibernate.atlassian.net/browse/HHH-20641")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditCollectionOverrideTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void createData(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			var child = new Child();
			child.id = 1L;
			child.name = "child";
			session.persist( child );
			var owner = new Owner();
			owner.id = 1L;
			owner.reference = 101L;
			owner.name = "owner";
			owner.restoredTags.add( "restored" );
			owner.renamedTags.add( "renamed" );
			owner.droppedTags.add( "dropped" );
			owner.revokedTags.add( "hidden" );
			owner.children.add( child );
			owner.links.add( child );
			owner.mscTags.add( "mapped superclass" );
			owner.memberOnlyTags.add( "member table" );
			session.persist( owner );
		} );
		scope.inTransaction( session -> session.find( Owner.class, 1L ).restoredTags.add( "added" ) );
	}

	@AfterClassTemplate
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void auditTableNamesBelongToOwner(DomainModelScope scope) {
		assertTable( scope, "override_restored_tags_AUD" );
		assertTable( scope, "owner_renamed_tags" );
		assertTable( scope, "override_links_AUD" );
		assertTable( scope, "OverrideCollectionOwner_OverrideCollectionChild_AUD" );
		assertTable( scope, "msc_renamed_tags" );
		assertTable( scope, "member_only_tags" );
		assertNoTable( scope, "override_revoked_tags_AUD" );
		assertNoTable( scope, "member_msc_tags" );
		assertNoTable( scope, "member_renamed_tags" );
		assertNoTable( scope, "override_dropped_tags_AUD" );
		assertNoTable( scope, "ignored_dropped_tags" );
	}

	@Test
	void auditReadsRespectOverrides(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var owner = auditLog.find( Owner.class, 1L, 1 );
			assertThat( owner.restoredTags ).containsExactly( "restored" );
			assertThat( owner.renamedTags ).containsExactly( "renamed" );
			assertThat( owner.droppedTags ).isNull();
			assertThat( owner.revokedTags ).isNull();
			assertThat( owner.children ).extracting( child -> child.name ).containsExactly( "child" );
			assertThat( owner.links ).extracting( child -> child.name ).containsExactly( "child" );
			assertThat( owner.mscTags ).containsExactly( "mapped superclass" );
			assertThat( owner.memberOnlyTags ).containsExactly( "member table" );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var owner = session.createSelectionQuery( "from OverrideCollectionOwner where id = 1", Owner.class )
					.getSingleResult();
			assertThat( owner.restoredTags ).containsExactly( "restored" );
			assertThat( owner.revokedTags ).isNull();
		}
	}

	@Test
	void restoredCollectionOnlyChangeCreatesOwnerRevision(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createNativeQuery(
					"select count(*) from override_collection_owner_AUD where id = 1",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			assertThat( session.createNativeQuery(
					"select count(*) from override_restored_tags_AUD",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 2 ).openSession()) {
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.name ).isEqualTo( "owner" );
			assertThat( owner.restoredTags ).containsExactlyInAnyOrder( "restored", "added" );
			assertThat( owner.renamedTags ).containsExactly( "renamed" );
			assertThat( owner.droppedTags ).isNull();
		}
	}

	@Test
	void excludedCollectionOnlyChangeCreatesNoOwnerRevision(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var owner = new Owner();
			owner.id = 3L;
			owner.reference = 103L;
			owner.revokedTags.add( "hidden" );
			session.persist( owner );
		} );
		// Excluding the collection also disables its optimistic locking, so the owner is not dirty
		scope.inTransaction( session -> session.find( Owner.class, 3L ).revokedTags.add( "still hidden" ) );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select count(*) from override_collection_owner_AUD where id = 3",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	void propertyRefCollectionChangeMergesWithOwnerChange(SessionFactoryScope scope) {
		persistPropertyRefOwner( scope, 4L );
		// The collection key is not the owner id, the owner revision must still merge with the owner update
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 4L );
			owner.name = "changed";
			owner.propertyRefTags.add( "second" );
		} );
		scope.inSession( session -> assertThat( session.createNativeQuery(
				"select count(*) from override_collection_owner_AUD where id = 4",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	static void persistPropertyRefOwner(SessionFactoryScope scope, long id) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = id;
			owner.reference = 100L + id;
			owner.propertyRefTags.add( "first" );
			session.persist( owner );
		} );
	}

	@Test
	void currentStateCollectionsIgnoreAuditExclusions(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.revokedTags ).containsExactly( "hidden" );
			assertThat( owner.droppedTags ).containsExactly( "dropped" );
		} );
	}

	@MappedSuperclass
	@Audited.Override(
			name = "mscTags",
			collectionTable = @Audited.CollectionTable(name = "msc_renamed_tags")
	)
	static class CollectionBase {
		@ElementCollection
		@CollectionTable(name = "override_msc_tags")
		@Audited.CollectionTable(name = "member_msc_tags")
		Set<String> mscTags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "override_restored_tags")
		@Audited.Excluded
		Set<String> restoredTags = new HashSet<>();

		@OneToMany
		@JoinColumn(name = "owner_id")
		@Audited.Excluded
		List<Child> children = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "override_links")
		@Audited.Excluded
		Set<Child> links = new HashSet<>();
	}

	@Entity(name = "OverrideCollectionOwner")
	@Table(name = "override_collection_owner")
	@Audited
	@Audited.Override(
			name = "renamedTags",
			collectionTable = @Audited.CollectionTable(name = "owner_renamed_tags")
	)
	@Audited.Override(
			name = "droppedTags",
			isAudited = false,
			collectionTable = @Audited.CollectionTable(name = "ignored_dropped_tags")
	)
	@Audited.Override(name = "memberOnlyTags")
	@Audited.Override(name = "restoredTags")
	@Audited.Override(name = "children")
	@Audited.Override(name = "links")
	@Audited.Override(name = "revokedTags", isAudited = false)
	static class Owner extends CollectionBase {
		@Id
		long id;
		String name;

		@Column(name = "owner_ref", unique = true)
		Long reference;

		@ElementCollection
		@CollectionTable(
				name = "override_property_ref_tags",
				joinColumns = @JoinColumn(name = "owner_ref", referencedColumnName = "owner_ref")
		)
		Set<String> propertyRefTags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "override_member_only_tags")
		@Audited.CollectionTable(name = "member_only_tags")
		Set<String> memberOnlyTags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "override_revoked_tags", joinColumns = @JoinColumn(name = "owner_id"))
		Set<String> revokedTags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "override_renamed_tags")
		@Audited.CollectionTable(name = "member_renamed_tags")
		Set<String> renamedTags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "override_dropped_tags")
		Set<String> droppedTags = new HashSet<>();
	}

	@Entity(name = "OverrideCollectionChild")
	@Table(name = "override_collection_child")
	@Audited
	static class Child {
		@Id
		long id;
		String name;
	}
}
