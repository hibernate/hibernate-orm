//$Id$
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for all tests.
 * 
 * @author Emmnauel Bernand
 * @author Hardy Ferentschik
 */
public abstract class TestCase extends junit.framework.TestCase {

	public static final Logger log = LoggerFactory.getLogger(TestCase.class);

	private static SessionFactory sessions;
	private static AnnotationConfiguration cfg;
	private static Dialect dialect;
	private static Class<?> lastTestClass;
	private Session session;

	/**
	 * The test method.
	 */
	private Method runMethod = null;

	/**
	 * Flag indicating whether the test should be skipped.
	 */
	private boolean skip = false;

	public TestCase() {
		super();
	}

	public TestCase(String x) {
		super(x);
	}

	protected void buildSessionFactory( Class<?>[] classes, String[] packages, String[] xmlFiles ) throws Exception {

		if ( getSessions() != null )
			getSessions().close();
		try {
			setCfg(new AnnotationConfiguration());
			configure(cfg);
			if ( recreateSchema() ) {
				cfg.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
			}
			for ( int i = 0; i < packages.length; i++ ) {
				getCfg().addPackage(packages[i]);
			}
			for ( int i = 0; i < classes.length; i++ ) {
				getCfg().addAnnotatedClass(classes[i]);
			}
			for ( int i = 0; i < xmlFiles.length; i++ ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlFiles[i]);
				getCfg().addInputStream(is);
			}
			setDialect(Dialect.getDialect());
			setSessions(getCfg().buildSessionFactory( /* new TestInterceptor() */));
		} catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected void setUp() throws Exception {
		runMethod = findTestMethod();
		checkSkip(runMethod);
		if ( !skip ) {
			if ( getSessions() == null || lastTestClass != getClass() ) {
				buildSessionFactory(getMappings(), getAnnotatedPackages(), getXmlFiles());
				lastTestClass = getClass();
			} else {
				runSchemaGeneration();
			}
		}
	}
	
	protected void runTest() throws Throwable {
		try {
			if ( !skip ) {
				runTestMethod(runMethod);
				handleUnclosedSession();
			}
		} catch ( Throwable e ) {
			closeSession(e);
		}
	}

	private void checkSkip( Method runMethod ) {
		Set<Class<? extends Dialect>> dialectList = new HashSet<Class<? extends Dialect>>();

		RequiresDialect requiresDialectMethodAnn = runMethod.getAnnotation(RequiresDialect.class);
		if ( requiresDialectMethodAnn != null ) {
			Class<? extends Dialect>[] requiredDialects = requiresDialectMethodAnn.value();
			dialectList.addAll(Arrays.asList(requiredDialects));
		}

		RequiresDialect requiresDialectClassAnn = getClass().getAnnotation(RequiresDialect.class);
		if ( requiresDialectClassAnn != null ) {
			Class<? extends Dialect>[] requiredDialects = requiresDialectClassAnn.value();
			dialectList.addAll(Arrays.asList(requiredDialects));
		}

		if ( dialectList.isEmpty() || dialectList.contains(Dialect.getDialect().getClass()) ) {
			skip = false;
		} else {
			log.warn("Skipping test {}, because test does not apply for dialect {}", runMethod.getName(), Dialect
					.getDialect().getClass());
			skip = true;
		}
	}

	private void runTestMethod( Method runMethod ) throws Throwable, IllegalAccessException {
		try {
			runMethod.invoke(this, new Class[0]);
		} catch ( InvocationTargetException e ) {
			e.fillInStackTrace();
			throw e.getTargetException();
		} catch ( IllegalAccessException e ) {
			e.fillInStackTrace();
			throw e;
		}
	}

	private Method findTestMethod() {
		String fName = getName();
		assertNotNull(fName);
		Method runMethod = null;
		try {
			runMethod = getClass().getMethod(fName, null);
		} catch ( NoSuchMethodException e ) {
			fail("Method \"" + fName + "\" not found");
		}
		if ( !Modifier.isPublic(runMethod.getModifiers()) ) {
			fail("Method \"" + fName + "\" should be public");
		}
		return runMethod;
	}

	private void handleUnclosedSession() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() )
				session.doWork(new RollbackWork());
			session.close();
			session = null;
			fail("unclosed session");
		} else {
			session = null;
		}
	}

	private void closeSession( Throwable e ) throws Throwable {
		try {
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() )
					session.doWork(new RollbackWork());
				session.close();
			}
		} catch ( Exception ignore ) {
		}
		try {
			if ( sessions != null ) {
				sessions.close();
				sessions = null;
			}
		} catch ( Exception ignore ) {
		}
		throw e;
	}

	public Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public Session openSession( Interceptor interceptor ) throws HibernateException {
		session = getSessions().openSession(interceptor);
		return session;
	}

	protected abstract Class<?>[] getMappings();

	protected String[] getAnnotatedPackages() {
		return new String[] {};
	}

	protected String[] getXmlFiles() {
		return new String[] {};
	}

	private void setSessions( SessionFactory sessions ) {
		TestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	private void setDialect( Dialect dialect ) {
		TestCase.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	protected static void setCfg( AnnotationConfiguration cfg ) {
		TestCase.cfg = cfg;
	}

	protected static AnnotationConfiguration getCfg() {
		return cfg;
	}

	protected void configure( Configuration cfg ) {
	}

	protected boolean recreateSchema() {
		return true;
	}

	protected void runSchemaGeneration() {
		SchemaExport export = new SchemaExport(cfg);
		export.create(true, true);
	}

	public class RollbackWork implements Work {

		public void execute( Connection connection ) throws SQLException {
			connection.rollback();
		}
	}
}
