/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.derivedid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = {
		ColumnLengthTest.Employee.class,
		ColumnLengthTest.Dependent.class
})
public class ColumnLengthTest {

	@Test
	public void testColumnLengthsAreApplied(DomainModelScope modelScope, @TempDir File tempDir) throws Exception {
		final var scriptFile = new File( tempDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( true );
		metadata.validate();

		new SchemaExport()
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );

		final var commands = Files.readAllLines( scriptFile.toPath() );

		Assertions.assertTrue( checkCommandIsGenerated(
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

	@SuppressWarnings("unused")
	@Embeddable
	public static class EmployeeId implements Serializable {
		@Column(name = "first_name", length = 32)
		String firstName;
		@Column(name = "last_name", length = 10)
		String lastName;
	}

	@SuppressWarnings("unused")
	@Entity
	@Table(name = "EMPLOYEE")
	public static class Employee {
		@EmbeddedId
		EmployeeId id;
	}

	@SuppressWarnings("unused")
	@Embeddable
	public static class DependentId implements Serializable {
		String name;
		EmployeeId empPK;
	}

	@SuppressWarnings("unused")
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
