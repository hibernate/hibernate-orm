/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.intf;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for mapping interfaces as managed classes.
 *
 * @implNote This is something {@code hbm.xml} supported, and we want to make sure it fails in
 * a consistent manner.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class InterfaceMappingTests {
	@ServiceRegistry
	@Test
	void testInterfaceAsEntity(ServiceRegistryScope registryScope) {
		try (StandardServiceRegistry serviceRegistry = registryScope.getRegistry()) {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClasses( IPerson.class, Person.class )
					.buildMetadata();
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() ).startsWith( "Only classes (not interfaces) may be mapped as @Entity :" );
			assertThat( expected.getMessage() ).endsWith( IPerson.class.getName() );
		}
	}

	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@Test
	void testTransformedHbmXml(ServiceRegistryScope registryScope) {
		try (StandardServiceRegistry serviceRegistry = registryScope.getRegistry()) {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addResource( "mappings/models/hbm/intf/mapped-interface.hbm.xml" )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
		}
	}
}
