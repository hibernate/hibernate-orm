// $Id:$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.util.StringHelper;

/**
 * A base class for all tests.
 *
 * @author Emmnauel Bernand
 * @author Hardy Ferentschik
 */
public abstract class HibernateTestCase extends TestCase {

	public static final Logger log = LoggerFactory.getLogger( HibernateTestCase.class );

	protected static Configuration cfg;
	private static Class<?> lastTestClass;


	/**
	 * The test method.
	 */
	private Method runMethod = null;

	/**
	 * Flag indicating whether the test should be run or skipped.
	 */
	private boolean runTest = true;

	/**
	 * List of required dialect for the current {@code runMethod}. If the list is empty any dialect is allowed.
	 * Otherwise the current dialect or a superclass of the current dialect must be in the list.
	 */
	private final Set<Class<? extends Dialect>> requiredDialectList = new HashSet<Class<? extends Dialect>>();

	/**
	 * List of dialects for which the current {@code runMethod} should be skipped.
	 */
	private final Set<Class<? extends Dialect>> skipForDialectList = new HashSet<Class<? extends Dialect>>();

	public HibernateTestCase() {
		super();
	}

	public HibernateTestCase(String x) {
		super( x );
	}

	@Override
	protected void setUp() throws Exception {
		runMethod = findTestMethod();
		setRunTestFlag( runMethod );
		if ( runTest ) {
			if ( cfg == null || lastTestClass != getClass() ) {
				buildConfiguration();
				lastTestClass = getClass();
			}
			else {
				runSchemaGeneration();
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {
		runSchemaDrop();
		handleUnclosedResources();
	}

	protected void runTest() throws Throwable {
		if ( runTest ) {
			runTestMethod( runMethod );
		}
	}

	private void setRunTestFlag(Method runMethod) {
		updateRequiredDialectList( runMethod );
		updateSkipForDialectList( runMethod );

		if ( runForCurrentDialect() ) {
			runTest = true;
		}
		else {
			log.warn(
					"Skipping test {}, because test does not apply for dialect {}", runMethod.getName(), Dialect
							.getDialect().getClass()
			);
			runTest = false;
		}
	}

	private void updateRequiredDialectList(Method runMethod) {
		requiredDialectList.clear();

		RequiresDialect requiresDialectMethodAnn = runMethod.getAnnotation( RequiresDialect.class );
		if ( requiresDialectMethodAnn != null ) {
			Class<? extends Dialect>[] requiredDialects = requiresDialectMethodAnn.value();
			requiredDialectList.addAll( Arrays.asList( requiredDialects ) );
		}

		RequiresDialect requiresDialectClassAnn = getClass().getAnnotation( RequiresDialect.class );
		if ( requiresDialectClassAnn != null ) {
			Class<? extends Dialect>[] requiredDialects = requiresDialectClassAnn.value();
			requiredDialectList.addAll( Arrays.asList( requiredDialects ) );
		}
	}

	private void updateSkipForDialectList(Method runMethod) {
		skipForDialectList.clear();

		SkipForDialect skipForDialectMethodAnn = runMethod.getAnnotation( SkipForDialect.class );
		if ( skipForDialectMethodAnn != null ) {
			Class<? extends Dialect>[] skipDialects = skipForDialectMethodAnn.value();
			skipForDialectList.addAll( Arrays.asList( skipDialects ) );
		}

		SkipForDialect skipForDialectClassAnn = getClass().getAnnotation( SkipForDialect.class );
		if ( skipForDialectClassAnn != null ) {
			Class<? extends Dialect>[] skipDialects = skipForDialectClassAnn.value();
			skipForDialectList.addAll( Arrays.asList( skipDialects ) );
		}
	}

	protected boolean runForCurrentDialect() {
		boolean runTestForCurrentDialect = true;

		// check whether the current dialect is assignableFrom from any of the specified required dialects.
		for ( Class<? extends Dialect> dialect : requiredDialectList ) {
			if ( dialect.isAssignableFrom( Dialect.getDialect().getClass() ) ) {
				runTestForCurrentDialect = true;
				break;
			}
			runTestForCurrentDialect = false;
		}

		// check whether the current dialect is assignableFrom from any of the specified skip for dialects.
		for ( Class<? extends Dialect> dialect : skipForDialectList ) {
			if ( dialect.isAssignableFrom( Dialect.getDialect().getClass() ) ) {
				runTestForCurrentDialect = false;
				break;
			}
			runTestForCurrentDialect = true;
		}

		return runTestForCurrentDialect;
	}

	private void runTestMethod(Method runMethod) throws Throwable {
		boolean failureExpected = runMethod.getAnnotation( FailureExpected.class ) != null;
		try {
			runMethod.invoke( this, new Class[0] );
			if ( failureExpected ) {
				throw new FailureExpectedTestPassedException();
			}
		}
		catch ( FailureExpectedTestPassedException t ) {
			closeResources();
			throw t;
		}
		catch ( Throwable t ) {
			if ( t instanceof InvocationTargetException ) {
				t = ( ( InvocationTargetException ) t ).getTargetException();
			}
			if ( t instanceof IllegalAccessException ) {
				t.fillInStackTrace();
			}
			closeResources();
			if ( failureExpected ) {
				FailureExpected ann = runMethod.getAnnotation( FailureExpected.class );
				StringBuilder builder = new StringBuilder();
				if ( StringHelper.isNotEmpty( ann.message() ) ) {
					builder.append( ann.message() );
				}
				else {
					builder.append( "ignoring test methods annoated with @FailureExpected" );
				}
				if ( StringHelper.isNotEmpty( ann.issueNumber() ) ) {
					builder.append( " (" );
					builder.append( ann.issueNumber() );
					builder.append( ")" );
				}
				reportSkip( builder.toString(), "Failed with: " + t.toString() );
			}
			else {
				throw t;
			}
		}
	}

	private Method findTestMethod() {
		String fName = getName();
		assertNotNull( fName );
		Method runMethod = null;
		try {
			runMethod = getClass().getMethod( fName );
		}
		catch ( NoSuchMethodException e ) {
			fail( "Method \"" + fName + "\" not found" );
		}
		if ( !Modifier.isPublic( runMethod.getModifiers() ) ) {
			fail( "Method \"" + fName + "\" should be public" );
		}
		return runMethod;
	}

	protected abstract void buildConfiguration() throws Exception;

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract void handleUnclosedResources();

	protected abstract void closeResources();

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	protected Dialect getDialect() {
		return Dialect.getDialect();
	}

	protected static void setCfg(Configuration cfg) {
		HibernateTestCase.cfg = cfg;
	}

	protected static Configuration getCfg() {
		return cfg;
	}

	protected void configure(Configuration cfg) {
	}

	protected boolean recreateSchema() {
		return true;
	}

	protected void runSchemaGeneration() {
		SchemaExport export = new SchemaExport( cfg );
		export.create( true, true );
	}

	protected void runSchemaDrop() {
		SchemaExport export = new SchemaExport( cfg );
		export.drop( true, true );
	}

	protected void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder();
		builder.append( "*** skipping test [" );
		builder.append( runMethod.getDeclaringClass().getName() );
		builder.append( "." );
		builder.append( runMethod.getName() );
		builder.append( "] - " );
		builder.append( testDescription );
		builder.append( " : " );
		builder.append( reason );

		log.warn( builder.toString() );
	}

	public class RollbackWork implements Work {

		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

	private static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException() {
			super( "Test marked as @FailureExpected, but did not fail!" );
		}
	}
}
