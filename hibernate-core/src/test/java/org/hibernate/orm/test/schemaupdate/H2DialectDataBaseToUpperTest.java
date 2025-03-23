/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Test;

@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-13597")
public class H2DialectDataBaseToUpperTest extends BaseUnitTestCase {

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Test
	public void hibernateShouldStartUpWithH2AutoUpdateAndDatabaseToUpperFalse() {
		setUp( "false" );
		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Test
	public void hibernateShouldStartUpWithH2AutoUpdateAndDatabaseToUpperTrue() {
		setUp( "true" );
		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private void setUp(String databaseToUpper) {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.URL,
						"jdbc:h2:mem:databaseToUpper;DATABASE_TO_UPPER=" + databaseToUpper + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
				)
				.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
	}

	@After
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
