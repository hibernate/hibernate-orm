/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Verifies that setting org.hibernate.cfg.AvailableSettings#XML_MAPPING_ENABLED to
 * false actually ignores the mapping files.
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name= MappingSettings.XML_MAPPING_ENABLED, value = "false"))
public class XMLMappingDisabledTest {

	@Test
	public void xmlMappedEntityIsIgnored(ServiceRegistryScope registryScope) throws Exception {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( AnnotationEntity.class, XmlEntity.class )
				.addResource( "org/hibernate/orm/test/bootstrap/binding/hbm/BadMapping.xml" );

		// even though BadMapping.xml is invalid, this should be ok
		// because we disabled XML processing
		metadataSources.buildMetadata();
	}
}
