/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@org.hibernate.testing.orm.junit.ServiceRegistry
public class MigrationTest {
	@Test
	public void testSimpleColumnAddition(ServiceRegistryScope registryScope) {
		String resource1 = "org/hibernate/orm/test/schemaupdate/1_Version.hbm.xml";
		String resource2 = "org/hibernate/orm/test/schemaupdate/2_Version.hbm.xml";

		MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addResource( resource1 )
				.buildMetadata();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), v1metadata );

		final SchemaUpdate v1schemaUpdate = new SchemaUpdate();
		v1schemaUpdate.execute(
				EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ),
				v1metadata
		);

		v1schemaUpdate.getExceptions().forEach(
				e -> System.out.println( e.getCause().getMessage() )
		);

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		MetadataImplementor v2metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addResource( resource2 )
				.buildMetadata();

		final SchemaUpdate v2schemaUpdate = new SchemaUpdate();
		v2schemaUpdate.execute(
				EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ),
				v2metadata
		);

		v2schemaUpdate.getExceptions().forEach(
				e -> System.out.println( e.getCause().getMessage() )
		);

		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), v2metadata );

	}

	@Test
	public void testSimpleColumnTypeChange(ServiceRegistryScope registryScope) {
		String resource1 = "org/hibernate/orm/test/schemaupdate/1_Version.hbm.xml";
		String resource4 = "org/hibernate/orm/test/schemaupdate/4_Version.hbm.xml";

		MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addResource( resource1 )
				.buildMetadata();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), v1metadata );

		final SchemaUpdate v1schemaUpdate = new SchemaUpdate();
		v1schemaUpdate.execute(
				EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ),
				v1metadata
		);

		v1schemaUpdate.getExceptions().forEach(
				e -> System.out.println( e.getCause().getMessage() )
		);

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		MetadataImplementor v2metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addResource( resource4 )
				.buildMetadata();

		final SchemaUpdate v2schemaUpdate = new SchemaUpdate();
		v2schemaUpdate.execute(
				EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ),
				v2metadata
		);

		v2schemaUpdate.getExceptions().forEach(
				e -> System.out.println( e.getCause().getMessage() )
		);

		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), v2metadata );

	}

	@Test
	@JiraKey( value = "HHH-9713" )
	public void testIndexCreationViaSchemaUpdate(ServiceRegistryScope registryScope) {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( EntityWithIndex.class )
				.buildMetadata();

		// drop and then create the schema
		new SchemaExport().execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH, metadata );

		try {
			// update the schema
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			// drop the schema
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
	}

	@Entity( name = "EntityWithIndex" )
	@Table( name = "T_Entity_With_Index",indexes = @Index( columnList = "name" ) )
	public static class EntityWithIndex {
		@Id
		public Integer id;
		public String name;
	}

	@Test
	@JiraKey( value = "HHH-9550" )
	public void testSameTableNameDifferentExplicitSchemas(ServiceRegistryScope registryScope) {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( CustomerInfo.class )
				.addAnnotatedClass( PersonInfo.class )
				.buildMetadata();

		// drop and then create the schema
		new SchemaExport().execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH, metadata );

		try {
			// update the schema
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			// drop the schema
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
	}

	@Entity
	@Table( name = "PERSON", schema = "CRM" )
	public static class CustomerInfo {
		@Id
		private Integer id;
	}

	@Entity
	@Table( name = "PERSON", schema = "ERP" )
	public static class PersonInfo {
		@Id
		private Integer id;
	}


}
