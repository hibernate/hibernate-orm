/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
class OrmXmlGeneratedTest {

	@Test
	void testOrmXmlDefinedGenerated() {
		StandardServiceRegistry ssr = ServiceRegistryBuilder.buildServiceRegistry();

		try {
			MetadataSources ms = new MetadataSources( ssr );
			ms.addResource( "org/hibernate/orm/test/annotations/generated/ormXml/orm.xml" );

			Metadata metadata = ms.buildMetadata();

			PersistentClass entityBinding = metadata.getEntityBinding( Tractor.class.getName() );
			GeneratorCreator generator = entityBinding
					.getProperty( "serialNumber" )
					.getValueGeneratorCreator();

			assertThat( generator )
					.extracting( creator -> creator.createGenerator(
							new IdGeneratorCreationContext( (MetadataImplementor) metadata, entityBinding.getRootClass() )
					) )
					.satisfies( gen ->
							assertThat( gen.getEventTypes() )
									.containsExactly( EventType.INSERT, EventType.UPDATE, EventType.FORCE_INCREMENT )
					);
		}
		finally {
			ServiceRegistryBuilder.destroy( ssr );
		}
	}
}
