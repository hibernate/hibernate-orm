/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.index;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RequiresDialect(H2Dialect.class)
public class IndexesOrderTest {

	@Test
	@JiraKey("HHH-16953")
	// see https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a14862
	public void testCreatedIndexColumnsOrderedAsSpecified() throws Exception {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		try {
			File output = File.createTempFile( "update_script", ".sql" );
			output.deleteOnExit();

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( EntityA.class )
					.addAnnotatedClass( EntityB.class )
					.buildMetadata();
			metadata.orderColumns( true );
			metadata.validate();

			new SchemaExport()
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( false )
					.create( EnumSet.of( TargetType.SCRIPT ), metadata );

			String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
					.replace( System.lineSeparator(), "" );
			assertThat( fileContent ).contains( "constraint entity_a_index unique (entityb_id, name, is_active)" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "EntityA")
	@Table(
			name = "ENTITY_A",
			indexes = { @Index(name = "entity_a_index", columnList = "entityb_id,name,is_active", unique = true) }
	)
	public static class EntityA {

		@Id
		private Long id;


		@Column(name = "name")
		private String name;

		@Column(name = "is_active")
		private Boolean isActive;

		@ManyToOne
		@JoinColumn(name = "entityb_id")
		private EntityB entityB;
	}

	@Entity(name = "EntityB")
	@Table(name = "ENTITY_B")
	public static class EntityB {

		@Id
		private Long id;

		private String name;
	}
}
