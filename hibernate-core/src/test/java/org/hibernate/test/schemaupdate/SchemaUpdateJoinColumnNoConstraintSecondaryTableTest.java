/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue(jiraKey = "HHH-8805")
public class SchemaUpdateJoinColumnNoConstraintSecondaryTableTest extends BaseUnitTestCase {

	private static final String EXPECTED_SCRIPT =
			"    create table Child ( " +
			"       id bigint not null, " +
			"        some_fk bigint, " +
			"        primary key (id) " +
			"    ); " +
			" " +
			"    create table Parent ( " +
			"       id bigint not null, " +
			"        primary key (id) " +
			"    ); ";
	private static final String DELIMITER = ";";

	@Test
	public void test() throws Exception {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		try {
			File output = File.createTempFile( "update_script", ".sql" );
			output.deleteOnExit();

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Parent.class )
					.buildMetadata();
			metadata.validate();

			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( DELIMITER )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.SCRIPT ), metadata );

			String outputContent = new String(Files.readAllBytes(output.toPath()));

			assertFalse( outputContent.toLowerCase().contains( "foreign key" ) );

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "Parent")
	@SecondaryTable(
			name = "ParentDetails",
			foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT)
	)
	public static class Parent {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
