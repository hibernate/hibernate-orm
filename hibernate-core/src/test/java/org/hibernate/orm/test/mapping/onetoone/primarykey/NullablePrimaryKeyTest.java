/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone.primarykey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test harness for ANN-742.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class NullablePrimaryKeyTest {

	@Test
	public void testGeneratedSql() {

		Map settings = new HashMap();
		settings.putAll( Environment.getProperties() );
		settings.put( AvailableSettings.DIALECT, SQLServerDialect.class.getName() );

		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( settings );

		try {
			MetadataSources ms = new MetadataSources( serviceRegistry );
			ms.addAnnotatedClass( Address.class );
			ms.addAnnotatedClass( Person.class );

			final Metadata metadata = ms.buildMetadata();
			final List<String> commands = new SchemaCreatorImpl( serviceRegistry ).generateCreationCommands(
					metadata,
					false
			);
			String expectedMappingTableSql = "create table personAddress (address_id bigint, " +
					"person_id bigint not null, primary key (person_id))";

			assertEquals( expectedMappingTableSql, commands.get( 2 ), "Wrong SQL" );
		}
		catch (Exception e) {
			fail( e.getMessage() );
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
