/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.derivedid;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

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

		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();

		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Employee.class )
				.addAnnotatedClass( Dependent.class )
				.buildMetadata();
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
				"create table DEPENDENT (name varchar(255) not null, FK1 varchar(32) not null, FK2 varchar(10) not null, primary key (FK1, FK2, name));"
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
		@JoinColumns({
				@JoinColumn(name = "FK1", referencedColumnName = "first_name"),
				@JoinColumn(name = "FK2", referencedColumnName = "last_name")
		})
		@ManyToOne
		Employee emp;
	}

}
