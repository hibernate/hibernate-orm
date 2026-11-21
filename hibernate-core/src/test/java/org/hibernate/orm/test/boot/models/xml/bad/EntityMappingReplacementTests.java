package org.hibernate.orm.test.boot.models.xml.bad;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServiceRegistry
public class EntityMappingReplacementTests {
	@Test
	void testPartialMappingCanRenameEntity(ServiceRegistryScope scope) {
		assertMergedMapping( scope, "partial-renamed", "MergedEntity" );
	}

	@Test
	void testUnnamedPartialMappingRetainsEarlierName(ServiceRegistryScope scope) {
		assertMergedMapping( scope, "partial-unnamed", "OriginalEntity" );
	}

	@Test
	void testIdenticalCompleteMappingsInSeparateResources(ServiceRegistryScope scope) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addResource( resource( "first-complete" ) )
				.addResource( resource( "first-complete-copy" ) )
				.buildMetadata();
		assertEquals( 1, metadata.getEntityBindings().size() );
		final var entity = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertEquals( "OriginalEntity", entity.getJpaEntityName() );
		assertEquals( "original_id", entity.getIdentifier().getColumns().get( 0 ).getName() );
		assertEquals( "original_name", entity.getProperty( "name" ).getValue().getColumns().get( 0 ).getName() );
	}

	@Test
	void testCompleteMappingCannotReplaceEarlierMappingWithSameName(ServiceRegistryScope scope) {
		assertReplacementRejected( scope, "complete-same-name", "OriginalEntity", ", removed basic attribute 'name'" );
	}

	@Test
	void testUnnamedCompleteMappingCannotReplaceEarlierMapping(ServiceRegistryScope scope) {
		assertReplacementRejected( scope, "complete-unnamed", "SimpleEntity", "" );
	}

	@Test
	void testPersistenceUnitCompletenessAppliesToEveryDocument(ServiceRegistryScope scope) {
		assertReplacementRejected( scope, "global-complete", "OriginalEntity", ", removed basic attribute 'name'" );
	}

	private static void assertMergedMapping(ServiceRegistryScope scope, String mapping, String entityName) {
		final var metadata = sources( scope, mapping ).buildMetadata();
		assertEquals( 1, metadata.getEntityBindings().size() );
		final var entity = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertEquals( entityName, entity.getJpaEntityName() );
		assertEquals( "original_id", entity.getIdentifier().getColumns().get( 0 ).getName() );
		assertEquals( "merged_name", entity.getProperty( "name" ).getValue().getColumns().get( 0 ).getName() );
	}

	private static void assertReplacementRejected(
			ServiceRegistryScope scope,
			String mapping,
			String incomingName,
			String attributeDetail) {
		final var exception = assertThrows( MappingException.class, () -> sources( scope, mapping ).buildMetadata() );
		assertEquals( resource( mapping ), exception.getOrigin().getName() );
		assertEquals(
				"Conflicting complete XML mappings for entity class '" + SimpleEntity.class.getName()
						+ "': entity names 'OriginalEntity' and '" + incomingName
						+ "'" + attributeDetail + " [" + resource( mapping ) + "]",
				exception.getMessage()
		);
	}

	private static MetadataSources sources(ServiceRegistryScope scope, String mapping) {
		final String firstMapping = mapping.startsWith( "partial-" ) ? "first" : "first-complete";
		return new MetadataSources( scope.getRegistry() )
				.addResource( resource( firstMapping ) )
				.addResource( resource( mapping ) );
	}

	private static String resource(String mapping) {
		return "mappings/models/bad/entity-replacement-" + mapping + ".xml";
	}
}
