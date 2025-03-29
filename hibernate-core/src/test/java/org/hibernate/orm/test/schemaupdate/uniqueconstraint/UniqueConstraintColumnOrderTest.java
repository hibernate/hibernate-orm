/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-16943")
@RequiresDialect( H2Dialect.class )
class UniqueConstraintColumnOrderTest {

	@Test
	// see https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#uniqueconstraint
	public void testUniqueConstraintColumnOrder(@TempDir Path tempDir) throws Exception {
		PersistenceUnitDescriptor puDescriptor = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return List.of( TestEntity.class.getName() );
			}
		};

		Map<Object, Object> settings = Map.of( AvailableSettings.HBM2DDL_AUTO, "create" );
		EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder( puDescriptor, settings );

		Path ddlScript = tempDir.resolve( "ddl.sql" );

		try (EntityManagerFactory entityManagerFactory = emfBuilder.build()) {
			// we do not need the entityManagerFactory, but we need to build it in order to demonstrate the issue (HHH-16943)

			Metadata metadata = emfBuilder.metadata();

			new SchemaExport()
					.setHaltOnError( true )
					.setOutputFile( ddlScript.toString() )
					.setFormat( true )
					.create( EnumSet.of( TargetType.SCRIPT ), metadata );
		}

		String ddl = Files.readString( ddlScript );
		assertThat( ddl ).contains( "constraint uk_b_a unique (b, a)" );
	}

	@Entity
	@Table(uniqueConstraints = @UniqueConstraint(name = "uk_b_a", columnNames = { "b", "a" }))
	static class TestEntity {

		@Id
		private Long id;
		private String a;
		private String b;

	}
}
