package org.hibernate.temporal.audit.auditoverrides;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.MappingException;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServiceRegistry
@Jira("https://hibernate.atlassian.net/browse/HHH-20641")
class AuditOverrideValidationTest {
	@Test
	void blankNameFails(ServiceRegistryScope scope) {
		assertInvalid( scope, BlankName.class, "Blank", " ", BlankName.class );
	}

	@Test
	void unknownNameFails(ServiceRegistryScope scope) {
		assertInvalid( scope, UnknownName.class, "Unknown", "missing", UnknownName.class );
	}

	@Test
	void collectionTableOnlyOverrideAlsoValidatesName(ServiceRegistryScope scope) {
		assertInvalid( scope, UnknownCollectionName.class, "Unknown", "missingCollection", UnknownCollectionName.class );
	}

	@Test
	void identifierNameFails(ServiceRegistryScope scope) {
		assertInvalid( scope, IdentifierName.class, "identifier", "id", IdentifierName.class );
	}

	@Test
	void versionNameFails(ServiceRegistryScope scope) {
		assertInvalid( scope, VersionName.class, "version", "version", VersionName.class );
	}

	@Test
	void singleTableInheritedEntityPropertyFails(ServiceRegistryScope scope) {
		assertInvalid( scope, TagsSubclass.class, "inherited from entity", "tags", TagsOwner.class, TagsSubclass.class );
	}

	@Test
	void tablePerClassInheritedEntityPropertyFails(ServiceRegistryScope scope) {
		assertInvalid( scope, UnionSub.class, "inherited from entity", "name", UnionBase.class, UnionSub.class );
	}

	@Test
	void mappedSuperclassBetweenEntitiesInheritedPropertyFails(ServiceRegistryScope scope) {
		assertInvalid(
				scope,
				MiddleMappedSuperclass.class,
				"inherited from entity",
				"name",
				UpperEntity.class,
				LowerEntity.class
		);
	}

	@Test
	void genericSpecializationInheritedFromEntityFails(ServiceRegistryScope scope) {
		assertInvalid(
				scope,
				GenericSub.class,
				"inherited from entity '" + GenericBase.class.getName() + "'",
				"payload",
				GenericBase.class,
				GenericSub.class
		);
	}

	@Test
	void duplicateNameOnSameClassFails(ServiceRegistryScope scope) {
		assertInvalid( scope, DuplicateName.class, "Duplicate", "name", DuplicateName.class );
	}

	@Test
	void unknownNameOnUnauditedEntityIsIgnored(ServiceRegistryScope scope) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Unaudited.class ).buildMetadata();
		assertThat( metadata.getEntityBinding( Unaudited.class.getName() ).getAuxiliaryTable() ).isNull();
	}

	@Test
	void unknownNameOnUnauditedTemporalEntityIsIgnored(ServiceRegistryScope scope) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( UnauditedTemporal.class ).buildMetadata();
		assertThat( metadata.getEntityBinding( UnauditedTemporal.class.getName() ).getAuxiliaryTable() ).isNotNull();
	}

	@Test
	void blankNameOnMappedSuperclassFails(ServiceRegistryScope scope) {
		assertInvalid( scope, BlankMappedSuperclass.class, "Blank", " ", MappedEntity.class );
	}

	private static void assertInvalid(
			ServiceRegistryScope scope,
			Class<?> declaringClass,
			String kind,
			String property,
			Class<?>... entityClasses
	) {
		final var sources = new MetadataSources( scope.getRegistry() ).addAnnotatedClasses( entityClasses );
		final var exception = assertThrows( MappingException.class, sources::buildMetadata );
		assertThat( exception ).hasMessageContaining( kind )
				.hasMessageContaining( declaringClass.getName() )
				.hasMessageContaining( "'" + property + "'" );
	}

	@MappedSuperclass
	static class Base {
		@Id
		long id;
		String name;
	}

	@Entity(name = "OverrideBlankName")
	@Table(name = "override_blank_name")
	@Audited
	@Audited.Override(name = " ")
	static class BlankName extends Base {
	}

	@Entity(name = "OverrideUnknownName")
	@Table(name = "override_unknown_name")
	@Audited
	@Audited.Override(name = "missing", isAudited = false)
	static class UnknownName extends Base {
	}

	@Entity(name = "OverrideUnknownCollectionName")
	@Table(name = "override_unknown_collection")
	@Audited
	@Audited.Override(
			name = "missingCollection",
			collectionTable = @Audited.CollectionTable(name = "unused")
	)
	static class UnknownCollectionName extends Base {
	}

	@Entity(name = "OverrideIdentifierName")
	@Table(name = "override_identifier_name")
	@Audited
	@Audited.Override(name = "id", isAudited = false)
	static class IdentifierName extends Base {
	}

	@Entity(name = "OverrideTagsOwner")
	@Table(name = "override_tags_owner")
	@Audited
	static class TagsOwner extends Base {
		@ElementCollection
		Set<String> tags = new HashSet<>();
	}

	@Entity(name = "OverrideTagsSubclass")
	@Audited.Override(name = "tags", collectionTable = @Audited.CollectionTable(name = "unused"))
	static class TagsSubclass extends TagsOwner {
	}

	@Entity(name = "OverrideUnionBase")
	@Table(name = "override_union_base")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Audited
	static class UnionBase extends Base {
	}

	@Entity(name = "OverrideUnionSub")
	@Table(name = "override_union_sub")
	@Audited.Override(name = "name", isAudited = false)
	static class UnionSub extends UnionBase {
	}

	@Entity(name = "OverrideUpperEntity")
	@Table(name = "override_upper_entity")
	@Audited
	static class UpperEntity extends Base {
	}

	@MappedSuperclass
	@Audited.Override(name = "name", isAudited = false)
	static class MiddleMappedSuperclass extends UpperEntity {
	}

	@Entity(name = "OverrideLowerEntity")
	static class LowerEntity extends MiddleMappedSuperclass {
	}

	@Entity(name = "OverrideGenericBase")
	@Table(name = "override_generic_base")
	@Audited
	abstract static class GenericBase<T extends Serializable> extends Base {
		@Basic
		T payload;
	}

	@Entity(name = "OverrideGenericSub")
	@Audited.Override(name = "payload")
	static class GenericSub extends GenericBase<String> {
	}

	@Entity(name = "OverrideDuplicateName")
	@Table(name = "override_duplicate_name")
	@Audited
	@Audited.Override(name = "name")
	@Audited.Override(name = "name", isAudited = false)
	static class DuplicateName extends Base {
	}

	@Entity(name = "OverrideVersionName")
	@Table(name = "override_version_name")
	@Audited
	@Audited.Override(name = "version")
	static class VersionName extends Base {
		@Version
		int version;
	}

	@Entity(name = "OverrideUnaudited")
	@Table(name = "override_unaudited")
	@Audited.Override(name = "missing")
	static class Unaudited extends Base {
	}

	@Entity(name = "OverrideUnauditedTemporal")
	@Table(name = "override_unaudited_temporal")
	@Temporal
	@Audited.Override(name = "missing")
	static class UnauditedTemporal extends Base {
	}

	@MappedSuperclass
	@Audited.Override(name = " ")
	static class BlankMappedSuperclass extends Base {
	}

	@Entity(name = "OverrideMappedEntity")
	@Table(name = "override_mapped_entity")
	@Audited
	static class MappedEntity extends BlankMappedSuperclass {
	}
}
