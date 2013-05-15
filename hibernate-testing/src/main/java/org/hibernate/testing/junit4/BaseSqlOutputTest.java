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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.sql.Reference;
import org.hibernate.testing.sql.SqlComparator;
import org.hibernate.testing.sql.SqlObject;
import org.hibernate.testing.sql.SqlParser;
import org.hibernate.testing.sql.SqlVisitor;
import org.hibernate.testing.sql.SqlWalker;
import org.hibernate.testing.sql.Statement;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

/**
 * <p>
 * The superclass for test classes that test the SQL output of all other test classes within the enclosing project.  SQL output
 * will only be tested by subclasses of this class if the system property {@value #TEST_SQL_OUTPUT_PROPERTY} is set to
 * {@code true}.  If not set, these test subclasses will merely pass successfully without testing the output.
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
 * may appear in any order when order is semantically irrelevant.
 * <li>DDL statements may appear in any order, except that {@code ALTER} and {@code CREATE INDEX} statements must appear after
 * the corresponding {@code CREATE} statements for their referenced tables.
 * <li>SQL statements may be output by either @BeforeClass methods or the associated test.
 * <li>Comparisons between test results and expected test results are case-insensitive
 * </ul>
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
	private static final SqlComparator COMPARATOR = new SqlComparator();
	private static final String MISSING_EXPECTED_SQL = "Missing expected SQL";
	private static final String UNEXPECTED_SQL = "Unexpected SQL";

	public static final String TEST_SQL_OUTPUT_PROPERTY = "testSqlOutput";

	private final SqlParser parser = new SqlParser();
	private int failures;

	@Test
	public void testSqlOutput() throws Exception {
		if ( !Boolean.getBoolean( TEST_SQL_OUTPUT_PROPERTY ) ) {
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
					statements.add( parser.parse( sql ) );
				} catch ( Exception error ) {
					fail( error.getMessage() + "\n\tlogged at " + timestamp + " in file: " + outputFile + "\n\tparsing SQL: "
							+ sql );
				}
			}
			builder.setLength( 0 );
		}
	}

	private void compareTest( List< Statement > statements, List< Statement > expectedStatements ) throws Exception {
		if ( statements.get( 0 ) instanceof FailureExpectedStatement ) {
			return;
		}
		COMPARATOR.compare( statements, expectedStatements );
	}

	private void compareTestClass(
			File outputFile,
			Map< String, List< Statement > > statementsByTest,
			File expectedOutputFile,
			Map< String, List< Statement > > expectedStatementsByTest ) throws Exception {
		String testClass = outputFile.getName();
		testClass = testClass.substring( TEST_OUTPUT_FILE_PREFIX.length(), testClass.length() - TEST_OUTPUT_FILE_SUFFIX.length() );
		COMPARATOR.clear();
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
			compareTest( beforeClassStatements, beforeClassExpectedStatements );
		}
		for ( Entry< String, List< Statement > > entry : statementsByTestCopy.entrySet() ) {
			String test = entry.getKey();
			final List< Statement > statements = entry.getValue();
			if ( statements.get( 0 ) instanceof FailureExpectedStatement ) {
				continue;
			}
			statements.addAll( 0, beforeClassStatements );
			List< Statement > expectedStatements = expectedStatementsByTestCopy.get( test );
			if ( expectedStatements == null ) {
				expectedStatements = new ArrayList< Statement >();
			}
			expectedStatements.addAll( 0, beforeClassExpectedStatements );
			String qualifiedTest = testClass + '.' + test;
			if ( !expectedStatements.isEmpty() ) {
				compareTest( statements, expectedStatements );
			}
			// Remove unmatched statements that are due to references to other unmatched statements
			pruneRedundantUnmatchedStatements( statements );
			pruneRedundantUnmatchedStatements( expectedStatements );
			failIfNotEmpty( testClass, qualifiedTest, statements, UNEXPECTED_SQL );
			failIfNotEmpty( testClass, qualifiedTest, expectedStatements, MISSING_EXPECTED_SQL );
		}
	}

	private Map< String, List< Statement > > copy( Map< String, List< Statement > > statementsByTest ) {
		Map< String, List< Statement > > copy = new HashMap< String, List< Statement > >();
		for ( Entry< String, List< Statement > > entry : statementsByTest.entrySet() ) {
			copy.put( entry.getKey(), new ArrayList< Statement >( entry.getValue() ) );
		}
		return copy;
	}

	private void fail( String message ) {
		LOG.error( message );
		failures++;
	}

	private void failIfNotEmpty( String testClass, String qualifiedTest, List< Statement > statements, String messagePrefix ) {
		if ( statements.isEmpty() ) {
			return;
		}
		StringBuilder builder = new StringBuilder( messagePrefix + " for " + qualifiedTest + ':' );
		for ( Statement statement : statements ) {
			builder.append( "\n\t" ).append( statement );
		}
		fail( builder.toString() );
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

	private void pruneRedundantUnmatchedStatements( final List< Statement > statements ) {
		for ( final Iterator< Statement > iter = statements.iterator(); iter.hasNext(); ) {
			final Statement statement = iter.next();
			SqlWalker.INSTANCE.walk( new SqlVisitor() {

				@Override
				public boolean visit( Object object, SqlObject parent, Field field, int index ) {
					if ( !( object instanceof Reference ) ) {
						return true;
					}
					Reference ref = ( Reference ) object;
					if ( ref.referent == null ) {
						return true;
					}
					Statement refStatement = ref.statement();
					Statement referentStatement = ref.referent.statement();
					if ( refStatement != referentStatement && statements.contains( referentStatement ) ) {
						iter.remove();
						return false;
					}
					return true;
				}
			}, statement );
		}
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
