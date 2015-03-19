/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.schematoolsnaming;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that using {@code --namingdelegator=<naming_delegator_class_name>} works when
 * using standalone schema tools.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9467")
public class StandaloneSchemaToolsNamingTest extends BaseUnitTestCase {
	private final java.io.File OUTFILE = new java.io.File( "StandaloneSchemaToolsNamingTest.out" );

	@Test
	public void testDefaultNamingStrategyDelegator() {
		// using the default NamingStrategyDelegator, the collection table name should be "Matrix_mvalues"
		doValidTest(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml"
				},
				"Matrix_mvalues"
		);
	}

	@Test
	public void testImprovedNamingStrategyDelegator() {
		// using ImprovedNamingStrategyDelegator, the collection table name should be "Mtx_mvalues"
		doValidTest(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--namingdelegator=" + ImprovedNamingStrategyDelegator.DEFAULT_INSTANCE.getClass().getName()
				},
				"Mtx_mvalues"
		);
	}

	@Test
	public void testEJB3NamingStrategy() {
		// using EJB3NamingStrategy should be the same as using the default NamingStrategyDelegator, so
		// the collection table name should be "Matrix_mvalues"
		doValidTest(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--naming=" + EJB3NamingStrategy.INSTANCE.getClass().getName(),
				},
				"Matrix_mvalues"
		);
	}

	@Test
	public void testSchemaExportNamingAndNamingDelegatorSpecified() {
		OUTFILE.delete();
		// --naming and --namingdelegator cannot be used together.
		// when SchemaExport is used standalone, an exception should be logged and OUTFILE should not exist.
		SchemaExport.main(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--naming=" + EJB3NamingStrategy.INSTANCE.getClass().getName(),
						"--namingdelegator=" + ImprovedNamingStrategyDelegator.DEFAULT_INSTANCE.getClass().getName()
				}
		);
		assertFalse( OUTFILE.exists() );
	}

	@Test
	public void testSchemaUpdateNamingAndNamingDelegatorSpecified() {
		OUTFILE.delete();
		// --naming and --namingdelegator cannot be used together.
		// when SchemaUpdate is used standalone, an exception should be logged and OUTFILE should not exist.
		SchemaUpdate.main(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--naming=" + EJB3NamingStrategy.INSTANCE.getClass().getName(),
						"--namingdelegator=" + ImprovedNamingStrategyDelegator.DEFAULT_INSTANCE.getClass().getName()
				}
		);
		assertFalse( OUTFILE.exists() );
	}

	@Test
	public void testSchemaValidatorNamingAndNamingDelegatorSpecified() {
		// --naming and --namingdelegator cannot be used together.
		// when SchemaValidator is used standalone, an exception should be logged.
		SchemaValidator.main(
				new String[] {
						"--output=" + OUTFILE.getName(),
						"--config=org/hibernate/test/schematoolsnaming/hibernate.cfg.xml",
						"--naming=" + EJB3NamingStrategy.INSTANCE.getClass().getName(),
						"--namingdelegator=" + ImprovedNamingStrategyDelegator.DEFAULT_INSTANCE.getClass().getName()
				}
		);
	}
	private void doValidTest(String args[], String collectionTableNameExpected) {
		OUTFILE.delete();
		SchemaExport.main( args );
		assertTrue( OUTFILE.exists() );
		assertFileContainsTrimmedLineStartingWith( OUTFILE, collectionTableNameExpected );

		SchemaValidator.main( args );

		OUTFILE.delete();
		// SchemaUpdate should result in an empty file because there should be nothing to update.
		SchemaUpdate.main( args );
		assertFileIsEmpty( OUTFILE );

		dropSchema( args );

		OUTFILE.delete();
		// since schema was dropped, OUTFILE should now contain references to collectionTableNameExpected.
		SchemaUpdate.main( args );
		assertTrue( OUTFILE.exists() );
		assertFileContainsTrimmedLineStartingWith( OUTFILE, collectionTableNameExpected );

		SchemaValidator.main( args );

		dropSchema( args );
	}

	private void assertFileContainsTrimmedLineStartingWith(java.io.File file, String text) {

		try {
			BufferedReader input = new BufferedReader( new FileReader( file ) );
			String line;
			/*
			 * readLine is a bit quirky :
			 * it returns the content of a line MINUS the newline.
			 * it returns null only for the END of the stream.
			 * it returns an empty String if two newlines appear in a row.
			 */
			while ( ( line = input.readLine() ) != null ) {
				final String trimmedLine = line.trim();
				if ( trimmedLine.contains( text ) ) {
					return;
				}
			}
		}
		catch ( IOException e ) {
			fail(
					String.format(
							"Failed due to IOException checking file [%s] for a line containing: [%s]",
							file.getName(),
							text
					)
			);
		}
		fail(
				String.format(
						"File [%s] does not contain a line containing: [%s]",
						file.getName(),
						text
				)
		);
	}

	private void assertFileIsEmpty(java.io.File file) {
		try {
			FileReader fileReader = new FileReader( file );
			assertEquals( -1, fileReader.read() );
		}
		catch (IOException ex) {
			fail( ex.getMessage() );
		}
	}

	private void dropSchema(String[] args) {
		final String[] argsNew = Arrays.copyOf( args, args.length + 1 );
		argsNew[ args.length ] = "--drop";
		SchemaExport.main( argsNew );

	}
}
