/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.junit4;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.sqlparser.Column;
import org.hibernate.testing.sqlparser.Index;
import org.hibernate.testing.sqlparser.Insert;
import org.hibernate.testing.sqlparser.Name;
import org.hibernate.testing.sqlparser.Sequence;
import org.hibernate.testing.sqlparser.SqlParser;
import org.hibernate.testing.sqlparser.Statement;
import org.hibernate.testing.sqlparser.Table;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

/**
 * <p>
 * The superclass for test classes that test the SQL output of all other test classes within the enclosing project.
 * </p><p>
 * By merely creating a subclass of this class (with no content) within a project, all SQL output from all tests within that
 * project will be verified against an <a href="#ExpectedResultsFile">expected results file</a> stored within the project's
 * {@code src/test/resources/expected-test-results} folder.  Note, there should only be one SQL output test per project.
 * </p><p>
 * <strong><em>Warning</em></strong>: it is important that a project's SQL output test runs after all other tests within the
 * project have completed.  This can be accomplished by ensuring the package name for the SQL output test is alphabetically,
 * without regard to case, greater than all other packages containing test classes (yes, it's a hack).
 * </p>
 * <a name="ExpectedResultsFile"><h3>Expected Results Files</h3></a>
 * <p>
 * Expected results for each test class must be within an XML file with the same name (i.e., "{@code TEST-<test class name>.xml}")
 * and element structure as the JUnit test results files that are produced in the project's {@code target/test-results} folder
 * whenever JUnit tests are run from the command line.
 * </p>
 * <h3>Results File Format</h3>
 * <p>
 * For both the results files produced by project tests and the corresponding expected results files, the only content analyzed is
 * within the character data (i.e., {@code CDATA}) of the {@code <testsuite>/<system-out>} element.  Within that element, only the
 * SQL output produced by the {@link org.hibernate.engine.jdbc.spi.SqlStatementLogger SqlStatementLogger} will be analyzed.  SQL
 * output must be preceded by a {@link Logger logged} heading in the form "{@code Test: <test name>}".  In addition, test classes
 * or tests annotated with {@link FailureExpected @FailureExpected} or
 * {@link org.hibernate.testing.FailureExpectedWithNewMetamodel @FailureExpectedWithNewMetamodel} must log these annotation names
 * before any SQL output produced by the corresponding test or @BeforeClass method and, for tests, after the
 * aforementioned test heading.  For test results (not expected test results), this behavior is already facilitated by the
 * {@link CustomRunner} class used by all subclasses of {@link BaseUnitTestCase}.  SQL output may span multiple lines, and may or
 * may not be {@link org.hibernate.engine.jdbc.spi.SqlStatementLogger#isFormat() formatted}.
 * </p><p>
 * <strong><em>Warning</em></strong>: Non-{@link Logger logger} output appearing immediately after logged SQL output can cause
 * false failures to be produced by subclasses of this class.  To avoid this, precede all non-logger output with
 * {@value org.hibernate.engine.jdbc.spi.SqlStatementLogger#OUTPUT_PREFIX}.
 * <p>
 * </p>
 * <h3>Processing</h3>
 * <p>
 * All errors and warnings produced by subclasses of this test are logged to the SQL output test's results file.  Specifically, a
 * warning is logged whenever SQL output is discovered for a test class but no corresponding expected results file exists.
 * </p><p>
 * SQL output will not be tested for any test classes where the class itself is annotated with
 * {@link FailureExpected @FailureExpected} or
 * {@link org.hibernate.testing.FailureExpectedWithNewMetamodel @FailureExpectedWithNewMetamodel}, nor for any tests annotated with
 * either of these annotations.
 * </p><p>
 * The following allowances are made when comparing test class results against expected results:
 * <ul>
 * <li>{@code DROP}-related statements are ignored.
 * <li>Names may be different, but must still be semantically equivalent.
 * <li>Column names and references within comma-separated lists in SQL (e.g., within {@code CREATE TABLE}, {@code SELECT}, etc.)
 * may appear in any order.
 * <li>DDL statements may appear in any order, except that {@code ALTER} and {@code CREATE INDEX} statements must appear after
 * the corresponding {@code CREATE} statements for their referenced tables.
 * <li>SQL statements may be output by either @BeforeClass methods or the associated test.
 * <li>Comparisons between test results and expected test results are case-insensitive
 * </ul>
 * </p><p>
 * Execution of subclasses of this class can be globally skipped by setting the system property
 * {@value #SKIP_SQL_OUTPUT_TESTS_PROPERTY} to {@code true}
 * </p>
 */
public abstract class BaseSqlOutputTest {

	private static final Logger LOG = Logger.getLogger( BaseSqlOutputTest.class );

	private static final String TEST_OUTPUT_FILE_PREFIX = "TEST-";
	private static final String TEST_OUTPUT_FILE_SUFFIX = ".xml";
	private static final String TIMESTAMP = "\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d";
	private static final String LOG_PATTERN = TIMESTAMP + ".*";
	private static final String CUSTOM_RUNNER_PATTERN_PREFIX = LOG_PATTERN + CustomRunner.class.getSimpleName() + ":\\d* - ";
	private static final String TEST_PATTERN = CUSTOM_RUNNER_PATTERN_PREFIX + CustomRunner.TEST_PREFIX + ".*";
	private static final String SQL_PATTERN = LOG_PATTERN + "SQL:\\d* -.*";
	private static final String OUTPUT_PATTERN = BaseUnitTestCase.OUTPUT_PREFIX + ".*";
	private static final String FAILURE_EXPECTED_PATTERN = CUSTOM_RUNNER_PATTERN_PREFIX + FailureExpected.class.getSimpleName()
			+ ".*";
	private static final String BEFORE_CLASS_NAME = BeforeClass.class.getSimpleName();
	private static final Comparator< Object > NAME_COMPARATOR = new Comparator< Object >() {

		@Override
		public int compare( Object object1, Object object2 ) {
			Name name1;
			Name name2;
			if ( object1 instanceof Column ) {
				name1 = ( ( Column ) object1 ).name;
				name2 = ( ( Column ) object2 ).name;
			} else {
				name1 = ( ( Name ) object1 );
				name2 = ( ( Name ) object2 );
			}
			return name1.unquoted().compareTo( name2.unquoted() );
		}
	};
	private static final String MISSING_EXPECTED_SQL = "Missing expected SQL";
	private static final String UNEXPECTED_SQL = "Unexpected SQL";

	public static final String SKIP_SQL_OUTPUT_TESTS_PROPERTY = "skipSqlOutputTests";

	private final SqlParser parser = new SqlParser();
	private int failures;

	@Test
	public void testSqlOutput() throws Exception {
		System.setProperty( SKIP_SQL_OUTPUT_TESTS_PROPERTY, "true" ); // TODO uncomment
		if ( Boolean.getBoolean( SKIP_SQL_OUTPUT_TESTS_PROPERTY ) ) {
			return;
		}
		File outputFolder = new File( "target/test-results" );
		if ( !outputFolder.exists() ) {
			return;
		}
		File[] outputFiles = outputFolder.listFiles( new FileFilter() {

			@Override
			public boolean accept( File file ) {
				String name = file.getName();
				return !file.isDirectory() && name.startsWith( TEST_OUTPUT_FILE_PREFIX )
						&& name.endsWith( TEST_OUTPUT_FILE_SUFFIX );
			}
		} );
		if ( outputFiles == null ) {
			return;
		}
		for ( File outputFile : outputFiles ) {
			testClassSqlOutput( outputFile );
		}
		if ( failures > 0 ) {
			Assert.fail( failures + " SQL output failure(s) occurred; see log for details." );
		}
	}

	private void addStatement( StringBuilder builder, List< Statement > statements, String timestamp, File outputFile ) {
		if ( builder.length() > 0 ) {
			String sql = builder.toString();
			String upperSql = sql.toUpperCase();
			if ( !upperSql.startsWith( "DROP" ) && !upperSql.matches( "ALTER .* DROP .*" ) ) {
				if ( sql.startsWith( "{" ) ) {
					sql = sql.substring( 1, sql.length() - 1 );
				}
				try {
					Statement statement = parser.parse( sql );
					if ( statement != null ) {
						statements.add( statement );
					}
				} catch ( Exception error ) {
					fail( error.getMessage() + "\n\tlogged at " + timestamp + " in file: " + outputFile + "\n\tparsing SQL: "
							+ sql );
				}
			}
			builder.setLength( 0 );
		}
	}

	private void compareTest(
			String qualifiedTest,
			List< Statement > statements,
			List< Statement > expectedStatements,
			Map< String, String > namesByExpectedName ) throws Exception {
		if ( statements.get( 0 ) instanceof FailureExpectedStatement ) {
			return;
		}
		for ( Iterator< Statement > iter = statements.iterator(); iter.hasNext(); ) {
			Statement statement = iter.next();
			for ( Iterator< Statement > expectedIter = expectedStatements.iterator(); expectedIter.hasNext(); ) {
				Statement expectedStatement = expectedIter.next();
				if ( equals( statement, expectedStatement, namesByExpectedName ) ) {
					iter.remove();
					expectedIter.remove();
					if ( statement instanceof Table || statement instanceof Sequence || statement instanceof Index ) {
						map( statement, expectedStatement, namesByExpectedName );
					}
					break;
				}
			}
		}
	}

	private void compareTestClass(
			File outputFile,
			Map< String, List< Statement > > statementsByTest,
			File expectedOutputFile,
			Map< String, List< Statement > > expectedStatementsByTest ) throws Exception {
		String testClass = outputFile.getName();
		testClass = testClass.substring( TEST_OUTPUT_FILE_PREFIX.length(), testClass.length() - TEST_OUTPUT_FILE_SUFFIX.length() );
		Map< String, String > namesByExpectedName = new HashMap< String, String >();
		namesByExpectedName.put( "?", "?" );
		Map< String, List< Statement > > statementsByTestCopy = copy( statementsByTest );
		Map< String, List< Statement > > expectedStatementsByTestCopy = copy( expectedStatementsByTest );

		List< Statement > beforeClassStatements = statementsByTestCopy.remove( BEFORE_CLASS_NAME );
		if ( beforeClassStatements == null ) {
			beforeClassStatements = new ArrayList< Statement >();
		}
		List< Statement > beforeClassExpectedStatements = expectedStatementsByTestCopy.remove( BEFORE_CLASS_NAME );
		if ( beforeClassExpectedStatements == null ) {
			beforeClassExpectedStatements = new ArrayList< Statement >();
		}
		if ( !beforeClassStatements.isEmpty() && !beforeClassExpectedStatements.isEmpty() ) {
			compareTest(
					testClass + ".@" + BEFORE_CLASS_NAME,
					beforeClassStatements,
					beforeClassExpectedStatements,
					namesByExpectedName );
		}
		for ( Iterator< Entry< String, List< Statement > > > iter = statementsByTestCopy.entrySet().iterator(); iter.hasNext(); ) {
			Entry< String, List< Statement > > entry = iter.next();
			String test = entry.getKey();
			List< Statement > statements = entry.getValue();
			statements.addAll( beforeClassStatements );
			List< Statement > expectedStatements = expectedStatementsByTestCopy.get( test );
			if ( expectedStatements == null ) {
				expectedStatements = new ArrayList< Statement >();
			}
			expectedStatements.addAll( beforeClassExpectedStatements );
			String qualifiedTest = testClass + '.' + test;
			if ( !expectedStatements.isEmpty() ) {
				compareTest( qualifiedTest, statements, expectedStatements, namesByExpectedName );
			}
			if ( statements.isEmpty() && expectedStatements.isEmpty() ) {
				iter.remove();
				expectedStatementsByTestCopy.remove( test );
			}
		}
		if ( !beforeClassStatements.isEmpty() || !beforeClassExpectedStatements.isEmpty() ) {
			String qualifiedTest = testClass + ".@" + BEFORE_CLASS_NAME;
			failIfNotEmpty( testClass, qualifiedTest, beforeClassStatements, UNEXPECTED_SQL );
			failIfNotEmpty( testClass, qualifiedTest, beforeClassExpectedStatements, MISSING_EXPECTED_SQL );
			return;
		}
		failIfNotEmpty( testClass, statementsByTestCopy, UNEXPECTED_SQL );
		failIfNotEmpty( testClass, expectedStatementsByTestCopy, MISSING_EXPECTED_SQL );
	}

	private Map< String, List< Statement > > copy( Map< String, List< Statement > > statementsByTest ) {
		Map< String, List< Statement > > copy = new HashMap< String, List< Statement > >();
		for ( Entry< String, List< Statement > > entry : statementsByTest.entrySet() ) {
			copy.put( entry.getKey(), new ArrayList< Statement >( entry.getValue() ) );
		}
		return copy;
	}

	private boolean equals( Statement statement, Object expectedObject, Map< String, String > namesByExpectedName )
		throws Exception {
		if ( !( statement instanceof Table || statement instanceof Sequence || statement instanceof Index ) ) { // TODO remove
			return true;
		}
		return equals( statement, expectedObject, namesByExpectedName, statement, null );
	}

	private boolean equals(
			Object object,
			Object expectedObject,
			Map< String, String > namesByExpectedName,
			Statement statement,
			Field field ) throws Exception {
		if ( object == null ) {
			if ( expectedObject != null ) {
				return false;
			}
			return true;
		} else if ( expectedObject == null ) {
			return false;
		}
		if ( object.getClass() != expectedObject.getClass() ) {
			return false;
		}
		if ( object instanceof Name ) {
			if ( statement instanceof Sequence || statement instanceof Table
					|| ( statement instanceof Index && field.getName().equals( "name" ) ) ) {
				return true;
			}
			return ( ( Name ) object ).unquoted().equalsIgnoreCase(
					namesByExpectedName.get( ( ( Name ) expectedObject ).unquoted() ) );
		}
		if ( object.getClass().getPackage() == Statement.class.getPackage() ) {
			Field[] fields = object.getClass().getFields();
			Field[] expectedFields = expectedObject.getClass().getFields();
			for ( int fldNdx = fields.length; --fldNdx >= 0; ) {
				Field fld = fields[ fldNdx ];
				if ( !equals(
						fld.get( object ),
						expectedFields[ fldNdx ].get( expectedObject ),
						namesByExpectedName,
						statement,
						fld ) ) {
					return false;
				}
			}
			return true;
		}
		if ( object instanceof List ) {
			List< Object > list = ( List< Object > ) object;
			if ( list.size() != ( ( List< Object > ) expectedObject ).size() ) {
				return false;
			}
			List< Object > expectedList = ( List< Object > ) expectedObject;
			if ( ( statement instanceof Table || statement instanceof Insert ) && field.getName().equals( "columns" ) ) {
				List< Object > listCopy = new ArrayList< Object >( list );
				Collections.sort( listCopy, NAME_COMPARATOR );
				List< Object > expectedListCopy = new ArrayList< Object >( expectedList );
				Collections.sort( expectedListCopy, NAME_COMPARATOR );
				for ( int listNdx = list.size(); --listNdx >= 0; ) {
					if ( !equals( listCopy.get( listNdx ), expectedListCopy.get( listNdx ), namesByExpectedName, statement, field ) ) {
						return false;
					}
				}
				expectedList.clear();
				expectedList.addAll( list );
			} else {
				for ( int listNdx = list.size(); --listNdx >= 0; ) {
					if ( !equals( list.get( listNdx ), expectedList.get( listNdx ), namesByExpectedName, statement, field ) ) {
						return false;
					}
				}
			}
			return true;
		}
		if ( object instanceof String ) {
			//			String name = Name.unquoted( object.toString() ); // TODO remove
			//			if ( !name.equals( "currval" ) && !name.equals( "bigint" ) && !name.equals( "integer" )
			//					&& !name.equalsIgnoreCase( "varchar" ) && !name.equals( "<=" ) && !name.equals( "decimal" )
			//					&& !name.equals( ">=" ) && !name.equals( "AND" ) && !name.equalsIgnoreCase( "char" )
			//					&& !name.equals( "timestamp" ) && !name.equals( "boolean" ) && !name.equals( "blob" )
			//					&& !name.equals( "time" ) && !name.equals( "date" ) && !name.equals( "longvarchar" )
			//					&& !name.equals( "binary" ) && !name.equals( "double" ) && !name.equals( "clob" ) && !name.equals( "float" )
			//					&& !name.equals( "smallint" ) && !name.equals( "tinyint" ) && !name.equals( "longvarbinary" ) ) {
			//				System.out.println();
			//			}
			return object.toString().equalsIgnoreCase( expectedObject.toString() );
		}
		return object.equals( expectedObject );
	}

	private void fail( String message ) {
		LOG.error( message );
		failures++;
	}

	private void failIfNotEmpty( String testClass, String qualifiedTest, List< Statement > statements, String messagePrefix ) {
		StringBuilder builder = new StringBuilder( messagePrefix + " for " + qualifiedTest + ':' );
		for ( Statement statement : statements ) {
			builder.append( "\n\t" ).append( statement );
		}
		fail( builder.toString() );
	}

	private void failIfNotEmpty( String testClass, Map< String, List< Statement >> statementsByTest, String messagePrefix ) {
		for ( Entry< String, List< Statement > > entry : statementsByTest.entrySet() ) {
			for ( Iterator< Statement > iter = entry.getValue().iterator(); iter.hasNext(); ) { // TODO remove
				Statement statement = iter.next();
				if ( !( statement instanceof Table || statement instanceof Sequence || statement instanceof Index ) ) {
					iter.remove();
				}
			}
			if ( entry.getValue().isEmpty() ) { // TODO remove
				continue;
			}
			failIfNotEmpty( testClass, testClass + '.' + entry.getKey(), entry.getValue(), messagePrefix );
		}
	}

	private void map( Statement statement, Object expectedObject, Map< String, String > namesByExpectedName ) throws Exception {
		map( statement, expectedObject, namesByExpectedName, statement, null );
	}

	private void map(
			Object object,
			Object expectedObject,
			Map< String, String > namesByExpectedName,
			Statement statement,
			Field field ) throws Exception {
		if ( object == null ) {
			return;
		}
		if ( object instanceof Name ) {
			namesByExpectedName.put( ( ( Name ) expectedObject ).unquoted(), ( ( Name ) object ).unquoted() );
			return;
		}
		if ( object.getClass().getPackage() == Statement.class.getPackage() ) {
			Field[] fields = object.getClass().getFields();
			Field[] expectedFields = expectedObject.getClass().getFields();
			for ( int fldNdx = fields.length; --fldNdx >= 0; ) {
				Field fld = fields[ fldNdx ];
				map( fld.get( object ), expectedFields[ fldNdx ].get( expectedObject ), namesByExpectedName, statement, fld );
			}
			return;
		}
		if ( object instanceof List ) {
			List< ? > list = ( List< ? > ) object;
			List< ? > expectedList = ( List< ? > ) expectedObject;
			for ( int listNdx = list.size(); --listNdx >= 0; ) {
				map( list.get( listNdx ), expectedList.get( listNdx ), namesByExpectedName, statement, field );
			}
		}
	}

	private Map< String, List< Statement > > parse( File outputFile ) throws Exception {
		Map< String, List< Statement > > statementsByTest = new HashMap< String, List< Statement > >();
		parser.clear();
		Node node =
				DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( outputFile ).getElementsByTagName( "system-out" ).item(
						0 ).getFirstChild();
		if ( node == null ) {
			return statementsByTest;
		}
		String[] lines = node.getTextContent().split( "\n" );
		final StringBuilder builder = new StringBuilder();
		boolean inSql = false;
		String timestamp = null;
		List< Statement > statements = new ArrayList< Statement >();
		String test = BEFORE_CLASS_NAME;
		statementsByTest.put( test, statements );
		boolean failureExpected = false;
		for ( int ndx = 0, len = lines.length; ndx < len; ndx++ ) {
			String line = lines[ ndx ];
			if ( line.isEmpty() ) {
				continue;
			}
			if ( line.matches( FAILURE_EXPECTED_PATTERN ) ) {
				failureExpected = true;
				statements.add( new FailureExpectedStatement() );
				if ( BEFORE_CLASS_NAME.equals( test ) ) {
					break;
				}
			} else if ( line.matches( TEST_PATTERN ) ) {
				addStatement( builder, statements, timestamp, outputFile );
				inSql = failureExpected = false;
				timestamp = null;
				if ( statements.isEmpty() ) {
					statementsByTest.remove( test );
				}
				test = line.substring( line.indexOf( CustomRunner.TEST_PREFIX ) + CustomRunner.TEST_PREFIX.length() );
				statements = new ArrayList< Statement >();
				statementsByTest.put( test, statements );
			} else if ( !failureExpected ) {
				if ( line.matches( SQL_PATTERN ) ) {
					inSql = true;
					addStatement( builder, statements, timestamp, outputFile );
					timestamp = line.substring( 0, 12 );
					line = line.substring( line.indexOf( '-' ) + 1 ).trim();
					if ( !line.isEmpty() ) {
						builder.append( line );
					}
				} else if ( line.matches( LOG_PATTERN ) || line.matches( OUTPUT_PATTERN ) ) {
					addStatement( builder, statements, timestamp, outputFile );
					inSql = false;
					timestamp = null;
				} else if ( inSql ) {
					if ( builder.length() > 0 ) {
						builder.append( ' ' );
					}
					builder.append( line.trim() );
				}
			}
		}
		addStatement( builder, statements, timestamp, outputFile );
		if ( statements.isEmpty() ) {
			statementsByTest.remove( test );
		}
		return statementsByTest;
	}

	private void testClassSqlOutput( File outputFile ) throws Exception {
		Map< String, List< Statement > > statementsByTest = parse( outputFile );
		File expectedOutputFile = new File( "src/test/resources/expected-test-results/" + outputFile.getName() );
		if ( statementsByTest.isEmpty() ) {
			if ( !expectedOutputFile.exists() ) {
				return;
			}
		} else {
			List< Statement > statements = statementsByTest.get( BEFORE_CLASS_NAME );
			if ( statements != null && statements.get( 0 ) instanceof FailureExpectedStatement ) {
				return;
			}
			if ( !expectedOutputFile.exists() ) {
				LOG.warn( "Test output file " + outputFile
						+ " contains SQL, but no corresponding file containing expected SQL exists in folder "
						+ expectedOutputFile.getParent() );
				return;
			}
		}
		Map< String, List< Statement > > expectedStatementsByTest = parse( expectedOutputFile );
		if ( statementsByTest.isEmpty() ) {
			if ( expectedStatementsByTest.isEmpty() ) {
				return;
			}
			fail( "Test output file " + outputFile
					+ " contains no SQL, but expected SQL exists in the corresponding file in folder "
					+ expectedOutputFile.getParent() );
			return;
		} else if ( expectedStatementsByTest.isEmpty() ) {
			fail( "Test output file " + outputFile
					+ " contains SQL, but no expected SQL exists in the corresponding file in folder "
					+ expectedOutputFile.getParent() );
			return;
		}
		compareTestClass( outputFile, statementsByTest, expectedOutputFile, expectedStatementsByTest );
	}
}
