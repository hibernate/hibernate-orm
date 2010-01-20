//$Id$
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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
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
public abstract class TestCase extends junit.framework.TestCase {

	public static final Logger log = LoggerFactory.getLogger( TestCase.class );

	private static SessionFactory sessions;
	private static AnnotationConfiguration cfg;
	private static Class<?> lastTestClass;
	private Session session;

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

	public TestCase() {
		super();
	}

	public TestCase(String x) {
		super( x );
	}

	protected void buildSessionFactory(Class<?>[] classes, String[] packages, String[] xmlFiles) throws Exception {

		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new AnnotationConfiguration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : packages ) {
				getCfg().addPackage( aPackage );
			}
			for ( Class<?> aClass : classes ) {
				getCfg().addAnnotatedClass( aClass );
			}
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setSessions( getCfg().buildSessionFactory( /* new TestInterceptor() */ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected void setUp() throws Exception {
		runMethod = findTestMethod();
		setRunTestFlag( runMethod );
		if ( runTest ) {
			if ( getSessions() == null || lastTestClass != getClass() ) {
				buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
				lastTestClass = getClass();
			}
			else {
				runSchemaGeneration();
			}
		}
	}

	protected void runTest() throws Throwable {
		if ( runTest ) {
			runTestMethod( runMethod );
			handleUnclosedSession();
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
			closeSession();
			throw t;
		}
		catch ( InvocationTargetException e ) {
			e.fillInStackTrace();
			throw e.getTargetException();
		}
		catch ( IllegalAccessException e ) {
			e.fillInStackTrace();
			throw e;
		}
		catch ( Throwable t ) {
			closeSession();
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

	private void handleUnclosedSession() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
			session = null;
			fail( "unclosed session" );
		}
		else {
			session = null;
		}
	}

	private void closeSession() {
		try {
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					session.doWork( new RollbackWork() );
				}
				session.close();
			}
		}
		catch ( Exception ignore ) {
		}
		try {
			if ( sessions != null ) {
				sessions.close();
				sessions = null;
			}
		}
		catch ( Exception ignore ) {
		}
	}

	public Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		session = getSessions().openSession( interceptor );
		return session;
	}

	protected abstract Class<?>[] getMappings();

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	private void setSessions(SessionFactory sessions) {
		TestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	protected Dialect getDialect() {
		return Dialect.getDialect();
	}

	protected static void setCfg(AnnotationConfiguration cfg) {
		TestCase.cfg = cfg;
	}

	protected static AnnotationConfiguration getCfg() {
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

	protected void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder( );
		builder.append( "*** skipping test [" );
		builder.append( runMethod.getDeclaringClass().getName() );
		builder.append( "." );
		builder.append(runMethod.getName() );
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
