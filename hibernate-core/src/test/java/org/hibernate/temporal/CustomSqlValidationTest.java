package org.hibernate.temporal;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// A temporal or audited entity or collection may not have custom SQL.
@ServiceRegistry
class CustomSqlValidationTest {

	@Test
	void temporalEntity(ServiceRegistryScope scope) {
		assertRejected( scope, TemporalThing.class,
				"TemporalThing' is temporal and so may not be annotated '@SQLInsert', '@SQLUpdate', or '@SQLDelete'" );
	}

	@Test
	void auditedEntity(ServiceRegistryScope scope) {
		assertRejected( scope, AuditedThing.class,
				"AuditedThing' is audited and so may not be annotated '@SQLInsert', '@SQLUpdate', or '@SQLDelete'" );
	}

	@Test
	void temporalCollection(ServiceRegistryScope scope) {
		assertRejected( scope, TemporalTags.class,
				"TemporalTags.tags' is temporal and so may not be annotated '@SQLInsert', '@SQLUpdate', '@SQLDelete', or '@SQLDeleteAll'" );
	}

	@Test
	void auditedCollection(ServiceRegistryScope scope) {
		assertRejected( scope, AuditedTags.class,
				"AuditedTags.tags' is audited and so may not be annotated '@SQLInsert', '@SQLUpdate', '@SQLDelete', or '@SQLDeleteAll'" );
	}

	@Test
	void excludedCollection(ServiceRegistryScope scope) {
		new MetadataSources( scope.getRegistry() ).addAnnotatedClass( ExcludedTags.class )
				.buildMetadata().buildSessionFactory().close();
	}

	private static void assertRejected(ServiceRegistryScope scope, Class<?> entityClass, String message) {
		final var metadata = new MetadataSources( scope.getRegistry() ).addAnnotatedClass( entityClass ).buildMetadata();
		assertThatThrownBy( () -> metadata.buildSessionFactory().close() )
				.isInstanceOf( UnsupportedMappingException.class )
				.hasMessageContaining( message );
	}

	@Temporal
	@Entity(name = "TemporalThing")
	@SQLUpdate(sql = "update TemporalThing set name = ? where id = ?")
	static class TemporalThing {
		@Id
		long id;
		String name;
	}

	@Audited
	@Entity(name = "AuditedThing")
	@SQLDelete(sql = "delete from AuditedThing where id = ?")
	static class AuditedThing {
		@Id
		long id;
		String name;
	}

	@Entity(name = "TemporalTags")
	static class TemporalTags {
		@Id
		long id;
		@Temporal
		@ElementCollection
		@SQLDeleteAll(sql = "delete from TemporalTags_tags where TemporalTags_id = ?")
		Set<String> tags = new HashSet<>();
	}

	/// The collection of an audited entity is itself audited.
	@Audited
	@Entity(name = "AuditedTags")
	static class AuditedTags {
		@Id
		long id;
		@ElementCollection
		@SQLInsert(sql = "insert into AuditedTags_tags (AuditedTags_id, tags) values (?, ?)")
		Set<String> tags = new HashSet<>();
	}

	@Audited
	@Entity(name = "ExcludedTags")
	static class ExcludedTags {
		@Id
		long id;
		@Audited.Excluded
		@ElementCollection
		@SQLInsert(sql = "insert into ExcludedTags_tags (ExcludedTags_id, tags) values (?, ?)")
		Set<String> tags = new HashSet<>();
	}
}
