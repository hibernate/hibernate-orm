/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import org.jboss.logging.Logger;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9865")
public class ImplicitCompositeKeyJoinTest {
	private static final Logger LOGGER = Logger.getLogger( ImplicitCompositeKeyJoinTest.class );

	@Test
	public void testSchemaCreationSQLCommandIsGeneratedWithTheCorrectColumnSizeValues() throws Exception {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final org.hibernate.boot.Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Employee.class )
					.buildMetadata();

			boolean createTableEmployeeFound = false;

			final List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands(
					metadata,
					false
			);

			for ( String command : commands ) {
				LOGGER.info( command );
				if ( command.toLowerCase().matches( "^create( (column|row))? table employee.+" ) ) {
					final String[] columnsDefinition = getColumnsDefinition( command );

					for ( String columnsDefinition1 : columnsDefinition ) {
						checkColumnSize( columnsDefinition1 );
					}
					createTableEmployeeFound = true;
				}
			}
			assertTrue(
					"Expected create table command for Employee entity not found",
					createTableEmployeeFound
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private String[] getColumnsDefinition(String command) {
		String substring = command.toLowerCase().replaceAll( "create( (column|row))? table employee ", "" );
		substring = substring.substring( 0, substring.toLowerCase().indexOf( "primary key" ) );
		return substring.split( "\\," );
	}

	private void checkColumnSize(String s) {
		if ( s.toLowerCase().contains( "manager_age" ) ) {
			if ( !s.contains( "15" ) ) {
				fail( expectedMessage( "manager_age", 15, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "manager_birthday" ) ) {
			if ( !s.contains( "255" ) ) {
				fail( expectedMessage( "manager_birthday", 255, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "manager_name" ) ) {
			if ( !s.contains( "20" ) ) {
				fail( expectedMessage( "manager_name", 20, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "age" ) ) {
			if ( !s.contains( "15" ) ) {
				fail( expectedMessage( "age", 15, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "birthday" ) ) {
			if ( !s.contains( "255" ) ) {
				fail( expectedMessage( "birthday", 255, s ) );
			}
		}
		else if ( s.toLowerCase().contains( "name" ) ) {
			if ( !s.contains( "20" ) ) {
				fail( expectedMessage( "name", 20, s ) );
			}
		}
	}

	private String expectedMessage(String column_name, int size, String actual) {
		return "Expected " + column_name + " " + size + " but was " + actual;
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@EmbeddedId
		private EmployeeId id;

		@ManyToOne
		@JoinColumns(value = {}, foreignKey = @ForeignKey(NO_CONSTRAINT))
		private Employee manager;
	}

	@Embeddable
	public class EmployeeId implements Serializable {
		@Column(length = 15)
		public String age;

		@Column(length = 20)
		private String name;

		private String birthday;
	}
}
