package org.hibernate.orm.test.util.dtd;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, value = "true"))
@BaseUnitTest
public class EntityResolverTest {
	@Test
	public void testEntityIncludeResolution(ServiceRegistryScope registryScope) {
		// Parent.hbm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/test/util/dtd/child.xml">
		// which we are expecting the Hibernate custom entity resolver to be able to resolve
		// locally via classpath lookup.
		new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/util/dtd/Parent.hbm.xml" )
				.buildMetadata();
	}

	@Test
	public void testOrmXmlEntityIncludeResolution(ServiceRegistryScope registryScope) {
		// Parent.orm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/orm/test/util/dtd/child-entity.xml">
		final var metadata = new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/util/dtd/Parent.orm.xml" )
				.buildMetadata();
		assertNotNull( metadata.getEntityBinding( Child.class.getName() ) );
	}

	@Test
	public void testOrmXmlInternalEntityResolution(ServiceRegistryScope registryScope) {
		// InternalEntity.orm.xml uses the following internal entity as the package name:
		//		<!ENTITY package "org.hibernate.orm.test.util.dtd">
		final var metadata = new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/util/dtd/InternalEntity.orm.xml" )
				.buildMetadata();
		assertNotNull( metadata.getEntityBinding( Parent.class.getName() ) );
	}
}
