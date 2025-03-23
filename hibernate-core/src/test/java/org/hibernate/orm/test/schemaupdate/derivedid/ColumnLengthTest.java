/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.derivedid;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class ColumnLengthTest extends BaseUnitTestCase {

	private StandardServiceRegistry ssr;
	private File outputFile;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws Exception {
		outputFile = File.createTempFile( "update_script", ".sql" );
		outputFile.deleteOnExit();

		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();

		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Employee.class )
				.addAnnotatedClass( Dependent.class )
				.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testTheColumnsLenghtAreApplied() throws Exception {
		new SchemaExport()
				.setOutputFile( outputFile.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );

		List<String> commands = Files.readAllLines( outputFile.toPath() );

		assertTrue( checkCommandIsGenerated(
				commands,
				"create table DEPENDENT (FK2 varchar(10) not null, FK1 varchar(32) not null, name varchar(255) not null, primary key (FK1, FK2, name));"
		) );

	}

	boolean checkCommandIsGenerated(List<String> generatedCommands, String toCheck) {
		for ( String command : generatedCommands ) {
			if ( command.contains( toCheck ) ) {
				return true;
			}
		}
		return false;
	}

	@Embeddable
	public class EmployeeId implements Serializable {
		@Column(name = "first_name", length = 32)
		String firstName;
		@Column(name = "last_name", length = 10)
		String lastName;
	}

	@Entity
	@Table(name = "EMLOYEE")
	public static class Employee {
		@EmbeddedId
		EmployeeId id;
	}

	@Embeddable
	public class DependentId implements Serializable {
		String name;
		EmployeeId empPK;
	}

	@Entity
	@Table(name = "DEPENDENT")
	public static class Dependent {
		@EmbeddedId
		DependentId id;
		@MapsId("empPK")
		@JoinColumn(name = "FK1", referencedColumnName = "first_name")
		@JoinColumn(name = "FK2", referencedColumnName = "last_name")
		@ManyToOne
		Employee emp;
	}

}
