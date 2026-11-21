package org.hibernate.orm.test.util.dtd;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that, by default, mapping documents declaring entities are rejected, while a plain DOCTYPE is accepted.
 *
 * @see org.hibernate.cfg.MappingSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@BaseUnitTest
public class EntityResolverDisallowedTest {
	@Test
	public void testEntityIncludeRejectedByDefault(ServiceRegistryScope registryScope) {
		// Parent.orm.xml declares a DTD with the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/orm/test/util/dtd/child-entity.xml">
		// which must not be processed unless explicitly allowed.
		assertRejected( registryScope, "org/hibernate/orm/test/util/dtd/Parent.orm.xml" );
	}

	@Test
	public void testInternalEntityRejectedByDefault(ServiceRegistryScope registryScope) {
		// InternalEntity.orm.xml declares a DTD with the following internal entity:
		//		<!ENTITY package "org.hibernate.orm.test.util.dtd">
		// which, not being external, is still not expanded unless explicitly allowed.
		assertRejected( registryScope, "org/hibernate/orm/test/util/dtd/InternalEntity.orm.xml" );
	}

	@Test
	public void testDoctypeWithoutEntitiesAcceptedByDefault(ServiceRegistryScope registryScope) throws Exception {
		// DoctypeWithoutEntities.orm.xml declares an unreachable external DTD and an internal subset
		// with no entities: the external DTD must not be fetched, and the document must still bind.
		try ( var stream = getClass().getResourceAsStream( "DoctypeWithoutEntities.orm.xml" ) ) {
			new MappingBinder( registryScope.getRegistry() ).bind( stream,
					new Origin( SourceType.RESOURCE, "DoctypeWithoutEntities.orm.xml" ) );
		}
	}

	private static void assertRejected(ServiceRegistryScope registryScope, String resource) {
		final var exception = assertThrows(
				Exception.class,
				() -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addMappingResource( resource ) )
		);
		final var mappingException = assertInstanceOf( MappingException.class, exception.getCause() );
		assertTrue(
				mappingException.getMessage().contains( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES ),
				mappingException.getMessage()
		);
	}
}
