package org.hibernate.orm.test.boot.models.xml.bad;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.models.MemberResolutionException;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class BadMappingTests {
	@Test
	@ServiceRegistry
	void testBadEmbeddedName(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/bad-embedded.xml" );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting failure" );
		}
		catch (MemberResolutionException expected){
			// expected outcome
		}

	}

	@Test
	@ServiceRegistry
	void testBadEmbeddedIdName(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/bad-embedded-id.xml" );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting failure" );
		}
		catch (MemberResolutionException expected){
			// expected outcome
		}
	}

	@Test
	@ServiceRegistry
	void testDuplicateEntityClass(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-class.xml" );
		final MappingException exception = assertThrows( MappingException.class, metadataSources::buildMetadata );
		assertTrue( exception.getMessage().contains( "org.hibernate.orm.test.boot.models.xml.SimpleEntity" ) );
	}

	@Test
	@ServiceRegistry
	void testDuplicateEntityClassAcrossDocuments(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-second.xml" );
		final MappingException exception = assertThrows( MappingException.class, metadataSources::buildMetadata );
		assertTrue( exception.getMessage().contains( "org.hibernate.orm.test.boot.models.xml.SimpleEntity" ) );
	}

	@Test
	@ServiceRegistry
	void testRepeatedNamedEntityMapping(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" );
		assertDoesNotThrow( () -> {
			return metadataSources.buildMetadata();
		} );
	}

	@Test
	@ServiceRegistry
	void testRepeatedUnnamedEntityMapping(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-unnamed.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-unnamed.xml" );
		assertDoesNotThrow( () -> {
			return metadataSources.buildMetadata();
		} );
	}

	@Test
	@ServiceRegistry
	void testUnnamedAndNamedEntityMappings(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-unnamed.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" );
		final var metadata = metadataSources.buildMetadata();
		assertEquals(
				"FirstEntity",
				metadata.getEntityBinding( "org.hibernate.orm.test.boot.models.xml.SimpleEntity" ).getJpaEntityName()
		);
	}

	@Test
	@ServiceRegistry
	void testUnnamedMappingDoesNotHideConflictingNames(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-unnamed.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" )
				.addResource( "mappings/models/bad/duplicate-entity-second.xml" );
		final MappingException exception = assertThrows( MappingException.class, metadataSources::buildMetadata );
		assertTrue( exception.getMessage().contains( "org.hibernate.orm.test.boot.models.xml.SimpleEntity" ) );
	}

	@Test
	@ServiceRegistry
	void testUnnamedCompleteMappingCannotReplaceExplicitEntityName(ServiceRegistryScope registryScope) {
		final String resource = "mappings/models/bad/duplicate-entity-unnamed.xml";
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/bad/duplicate-entity-first.xml" )
				.addResource( resource );
		final MappingException exception = assertThrows( MappingException.class, metadataSources::buildMetadata );
		assertEquals( resource, exception.getOrigin().getName() );
		assertTrue( exception.getMessage().contains( "entity names 'FirstEntity' and 'SimpleEntity'" ) );
	}

	@Test
	@ServiceRegistry
	void testAnnotatedNameWithRepeatedUnnamedMappings(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( NamedEntity.class )
				.addResource( "mappings/models/bad/repeated-unnamed-entity.xml" )
				.addResource( "mappings/models/bad/repeated-unnamed-entity.xml" );
		final var metadata = assertDoesNotThrow( () -> {
			return metadataSources.buildMetadata();
		} );
		assertEquals( "CustomEntityName", metadata.getEntityBinding( NamedEntity.class.getName() ).getJpaEntityName() );
	}

	@jakarta.persistence.Entity(name = "CustomEntityName")
	public static class NamedEntity {
		@jakarta.persistence.Id
		private Integer id;
	}
}
