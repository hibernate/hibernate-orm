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

// $Id$

package org.hibernate.testing.junit.functional.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.junit.DialectChecks;
import org.hibernate.testing.junit.FailureExpected;
import org.hibernate.testing.junit.RequiresDialect;
import org.hibernate.testing.junit.RequiresDialectFeature;
import org.hibernate.testing.junit.SkipForDialect;
import org.hibernate.testing.junit.SkipLog;
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

	public HibernateTestCase() {
		super();
	}

	public HibernateTestCase(String x) {
		super( x );
	}

	@Override
	public void runBare() throws Throwable {
		Method runMethod = findTestMethod();

		final Skip skip = determineSkipByDialect( Dialect.getDialect(), runMethod );
		if ( skip != null ) {
			reportSkip( skip );
			return;
		}

		setUp();
		try {
			runTest();
		}
		finally {
			tearDown();
		}
	}

	@Override
	protected void runTest() throws Throwable {
		Method runMethod = findTestMethod();
		FailureExpected failureExpected = locateAnnotation( FailureExpected.class, runMethod );
		try {
			super.runTest();
			if ( failureExpected != null ) {
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
			if ( failureExpected != null ) {
				StringBuilder builder = new StringBuilder();
				if ( StringHelper.isNotEmpty( failureExpected.message() ) ) {
					builder.append( failureExpected.message() );
				}
				else {
					builder.append( "ignoring @FailureExpected test" );
				}
				builder.append( " (" )
						.append( failureExpected.jiraKey() )
						.append( ")" );
				SkipLog.LOG.warn( builder.toString(), t );
			}
			else {
				throw t;
			}
		}
	}

	@Override
	protected void setUp() throws Exception {
		if ( cfg == null || lastTestClass != getClass() ) {
			buildConfiguration();
			lastTestClass = getClass();
		}
		else {
			runSchemaGeneration();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		runSchemaDrop();
		handleUnclosedResources();
	}

	protected static class Skip {
		private final String reason;
		private final String testDescription;

		public Skip(String reason, String testDescription) {
			this.reason = reason;
			this.testDescription = testDescription;
		}
	}

	protected final Skip determineSkipByDialect(Dialect dialect, Method runMethod) throws Exception {
		// skips have precedence, so check them first
		SkipForDialect skipForDialectAnn = locateAnnotation( SkipForDialect.class, runMethod );
		if ( skipForDialectAnn != null ) {
			for ( Class<? extends Dialect> dialectClass : skipForDialectAnn.value() ) {
				if ( skipForDialectAnn.strictMatching() ) {
					if ( dialectClass.equals( dialect.getClass() ) ) {
						return buildSkip( dialect, skipForDialectAnn.comment(), skipForDialectAnn.jiraKey() );
					}
				}
				else {
					if ( dialectClass.isInstance( dialect ) ) {
						return buildSkip( dialect, skipForDialectAnn.comment(), skipForDialectAnn.jiraKey() );
					}
				}
			}
		}

		// then check against the requires
		RequiresDialect requiresDialectAnn = locateAnnotation( RequiresDialect.class, runMethod );
		if ( requiresDialectAnn != null ) {
			for ( Class<? extends Dialect> dialectClass : requiresDialectAnn.value() ) {
				if ( requiresDialectAnn.strictMatching() ) {
					if ( !dialectClass.equals( dialect.getClass() ) ) {
						return buildSkip( dialect, null, null );
					}
				}
				else {
					if ( !dialectClass.isInstance( dialect ) ) {
						return buildSkip( dialect, requiresDialectAnn.comment(), requiresDialectAnn.jiraKey() );
					}
				}
			}
		}

		// then check against a dialect feature
		RequiresDialectFeature requiresDialectFeatureAnn = locateAnnotation( RequiresDialectFeature.class, runMethod );
		if ( requiresDialectFeatureAnn != null ) {
			Class<? extends DialectChecks> checkClass = requiresDialectFeatureAnn.value();
			DialectChecks check = checkClass.newInstance();
			boolean skip = !check.include( dialect );
			if ( skip ) {
				return buildSkip( dialect, requiresDialectFeatureAnn.comment(), requiresDialectFeatureAnn.jiraKey() );
			}
		}
		return null;
	}

	protected <T extends Annotation> T locateAnnotation(Class<T> annotationClass, Method runMethod) {
		T annotation = runMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = getClass().getAnnotation( annotationClass );
		}
		if ( annotation == null ) {
			annotation = runMethod.getDeclaringClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	protected Skip buildSkip(Dialect dialect, String comment, String jiraKey) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "skipping database-specific test [" );
		buffer.append( fullTestName() );
		buffer.append( "] for dialect [" );
		buffer.append( dialect.getClass().getName() );
		buffer.append( ']' );

		if ( StringHelper.isNotEmpty( comment ) ) {
			buffer.append( "; " ).append( comment );
		}

		if ( StringHelper.isNotEmpty( jiraKey ) ) {
			buffer.append( " (" ).append( jiraKey ).append( ')' );
		}

		return new Skip( buffer.toString(), null );
	}

	public String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
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

	protected String[] getMappings() {
		return new String[] { };
	}

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

	private void reportSkip(Skip skip) {
		reportSkip( skip.reason, skip.testDescription );
	}

	protected void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder();
		builder.append( "*** skipping test [" );
		builder.append( fullTestName() );
		builder.append( "] - " );
		builder.append( testDescription );
		builder.append( " : " );
		builder.append( reason );
		SkipLog.LOG.warn( builder.toString() );
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
