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
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10197")
public class QuotedTableNameWithForeignKeysSchemaUpdateTest extends BaseUnitTestCase {

	@Before
	public void setUp() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testUpdateExistingSchema() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testGeneratingUpdateScript() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaUpdate().execute( EnumSet.of( TargetType.STDOUT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@After
	public void tearDown() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/schemaupdate/UserGroup.hbm.xml" )
					.buildMetadata();
			new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), metadata );

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
