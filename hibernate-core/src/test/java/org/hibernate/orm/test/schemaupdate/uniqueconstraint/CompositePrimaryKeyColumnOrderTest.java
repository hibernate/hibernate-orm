/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
import jakarta.persistence.IdClass;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-17065")
@RequiresDialect( H2Dialect.class )
class CompositePrimaryKeyColumnOrderTest {

	@Test
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
			// we do not need the entityManagerFactory, but we need to build it in order to demonstrate the issue (HHH-17065)

			Metadata metadata = emfBuilder.metadata();

			new SchemaExport()
					.setHaltOnError( true )
					.setOutputFile( ddlScript.toString() )
					.setFormat( true )
					.create( EnumSet.of( TargetType.SCRIPT ), metadata );
		}

		String ddl = Files.readString( ddlScript );
		assertThat( ddl ).contains( "primary key (b, a)" );
	}

	@Entity
	@Table(
			indexes = @Index(unique = true, columnList = "b, a")
	)
	@IdClass( CompositePrimaryKey.class )
	static class TestEntity {

		@Id
		private String b;

		@Id
		private String a;

	}

	private static class CompositePrimaryKey implements Serializable {

		private static final long serialVersionUID = 1L;

		private String b;

		private String a;

		public String getB() {
			return b;
		}

		public void setB(String b) {
			this.b = b;
		}

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CompositePrimaryKey that = (CompositePrimaryKey) o;
			return Objects.equals( getB(), that.getB() ) && Objects.equals( getA(), that.getA() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getB(), getA() );
		}
	}
}
