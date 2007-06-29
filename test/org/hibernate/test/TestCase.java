//$Id$
package org.hibernate.test;

import java.sql.Blob;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

public abstract class TestCase extends junit.framework.TestCase {

	private static SessionFactory sessions;
	private static Configuration cfg;
	private static Dialect dialect;
	private static Class lastTestClass;
	private org.hibernate.classic.Session session;

	public TestCase(String name) {
		super( name );
	}


	// methods for subclasses to change test environment ~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the mapping resources to be used to build the configuration.
	 * <p/>
	 * Resources should be relative to {@link #getBaseForMappings()}
	 *
	 * @return The mapping resources
	 */
	protected abstract String[] getMappings();

	/**
	 * The base name for relative mapping resources.  The default is
	 * <tt>org/hibernate/test/</tt>
	 *
	 * @return the mapping resource base
	 */
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	/**
	 * Should the database schema be (re)created
	 *
	 * @return True for auto export (including recreation on test failure).
	 */
	protected boolean recreateSchema() {
		return true;
	}

	protected boolean dropAfterFailure() {
		return true;
	}

	/**
	 * Apply any test-specific configuration prior to building the factory.
	 *
	 * @param cfg The configuration which will be used to construct the factory.
	 */
	protected void configure(Configuration cfg) {
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}


	// methods for subclasses to access environment ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the factory for this test environment.
	 *
	 * @return The factory.
	 */
	protected SessionFactory getSessions() {
		return sessions;
	}

	/**
	 * Get the factory for this test environment, casted to {@link SessionFactoryImplementor}.
	 * <p/>
	 * Shorthand for ( {@link SessionFactoryImplementor} ) {@link #getSessions()}...
	 *
	 * @return The factory
	 */
	protected SessionFactoryImplementor sfi() {
		return ( SessionFactoryImplementor ) getSessions();
	}

	protected Dialect getDialect() {
		if ( dialect == null ) {
			dialect = Dialect.getDialect();
		}
		return dialect;
	}

	protected Configuration getCfg() {
		return cfg;
	}

	public org.hibernate.classic.Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public org.hibernate.classic.Session openSession(Interceptor interceptor)
	throws HibernateException {
		session = getSessions().openSession(interceptor);
		return session;
	}


	// JUnit hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The Hibernate test suite tries to only build the db schema once
	 * per test class (not test case which = instance) hence all the
	 * static vars.
	 * <p/>
	 * Here is the crux of that attempt.  We only build a factory when one was
	 * not previously built, or when we start a new test class.
	 *
	 * @throws Exception
	 */
	protected void setUp() throws Exception {
		if ( getSessions() == null || lastTestClass != getClass() ) {
			buildSessionFactory();
			lastTestClass = getClass();
		}
	}


	private void buildSessionFactory() throws Exception {
		if ( getSessions()!=null ) {
			getSessions().close();
		}

		TestCase.dialect = Dialect.getDialect();
		if ( ! appliesTo( getDialect() ) ) {
			return;
		}

		try {

			TestCase.cfg = new Configuration();
			cfg.setProperty( Environment.CACHE_PROVIDER, "org.hibernate.cache.HashtableCacheProvider" );
			if( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			addMappings( getMappings(), cfg );
			configure( cfg );

			if ( getCacheConcurrencyStrategy() != null ) {
				Iterator iter = cfg.getClassMappings();
				while ( iter.hasNext() ) {
					PersistentClass clazz = (PersistentClass) iter.next();
					Iterator props = clazz.getPropertyClosureIterator();
					boolean hasLob = false;
					while ( props.hasNext() ) {
						Property prop = (Property) props.next();
						if ( prop.getValue().isSimpleValue() ) {
							String type = ( (SimpleValue) prop.getValue() ).getTypeName();
							if ( "blob".equals(type) || "clob".equals(type) ) hasLob = true;
							if ( Blob.class.getName().equals(type) || Clob.class.getName().equals(type) ) hasLob = true;
						}
					}
					if ( !hasLob && !clazz.isInherited() && overrideCacheStrategy() ) {
						cfg.setCacheConcurrencyStrategy(
								clazz.getEntityName(),
								getCacheConcurrencyStrategy()
							);
					}
				}
				iter = cfg.getCollectionMappings();
				while ( iter.hasNext() ) {
					Collection coll = (Collection) iter.next();
					cfg.setCollectionCacheConcurrencyStrategy(
							coll.getRole(),
							getCacheConcurrencyStrategy()
						);
				}
			}

			// make sure we use the same dialect...
			cfg.setProperty( Environment.DIALECT, TestCase.dialect.getClass().getName() );
			TestCase.sessions = cfg.buildSessionFactory();
			afterSessionFactoryBuilt();
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	protected void addMappings(String[] files, Configuration cfg) {
		for ( int i = 0; i < files.length; i++ ) {
			if ( !files[i].startsWith( "net/" ) ) {
				files[i] = getBaseForMappings() + files[i];
			}
			cfg.addResource( files[i], TestCase.class.getClassLoader() );
		}
	}

	protected void afterSessionFactoryBuilt() throws Exception {
		// for subclasses to override in order to perform extra "stuff" only
		// when SF (re)built...
	}

	protected void runTest() throws Throwable {
		final boolean stats = sessions.getStatistics().isStatisticsEnabled();
		try {
			if ( stats ) {
				sessions.getStatistics().clear();
			}

			super.runTest();

			if ( stats ) {
				sessions.getStatistics().logSummary();
			}

			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					session.connection().rollback();
				}
				session.close();
				session = null;
				fail( "unclosed session" );
			}
			else {
				session = null;
			}
			assertAllDataRemoved();
		}
		catch ( Throwable e ) {
			try {
				if ( session != null && session.isOpen() ) {
					if ( session.isConnected() ) {
						session.connection().rollback();
					}
					session.close();
				}
			}
			catch ( Exception ignore ) {
			}
			try {
				if ( dropAfterFailure() && sessions != null ) {
					sessions.close();
					sessions = null;
				}
			}
			catch ( Exception ignore ) {
			}
			throw e;
		}
	}

	public void runBare() throws Throwable {
		String sysPropName = "hibernate.test.validatefailureexpected";
		assertNotNull( getName() );
		if ( Boolean.getBoolean( sysPropName ) ) {
			if ( getName().endsWith( "FailureExpected" ) ) {
				Throwable t = null;
				try {
					super.runBare();
				}
				catch ( Throwable afe ) {
					t = afe;
				}
				if ( t == null ) {
					fail( "Test where marked as FailureExpected, but did not fail!" );
				}
				else {
					reportSkip( "ignoring *FailuredExpected methods", "Failed with: " + t.toString() );
				}
			}
			else {
				super.runBare();
			}
		}
		else {
			super.runBare();
		}
	}

	protected void assertAllDataRemoved() {
		if ( !recreateSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( "hibernate.test.validateDataCleanup" ) ) {
			return;
		}

		Session tmpSession = sessions.openSession();
		try {
			List list = tmpSession.createQuery( "select o from java.lang.Object o" ).list();

			Map items = new HashMap();
			if ( !list.isEmpty() ) {
				for ( Iterator iter = list.iterator(); iter.hasNext(); ) {
					Object element = iter.next();
					Integer l = ( Integer ) items.get( tmpSession.getEntityName( element ) );
					if ( l == null ) {
						l = new Integer( 0 );
					}
					l = new Integer( l.intValue() + 1 );
					items.put( tmpSession.getEntityName( element ), l );
					System.out.println( "Data left: " + element );
				}
				fail( "Data is left in the database: " + items.toString() );
			}
		}
		finally {
			try {
				tmpSession.close();
			}
			catch( Throwable t ) {
				// intentionally empty
			}
		}
	}

	public static void assertElementTypeAssignability(java.util.Collection collection, Class clazz) throws AssertionFailedError {
		Iterator itr = collection.iterator();
		while ( itr.hasNext() ) {
			assertClassAssignability( itr.next().getClass(), clazz );
		}
	}

	public static void assertClassAssignability(Class source, Class target) throws AssertionFailedError {
		if ( !target.isAssignableFrom( source ) ) {
			throw new AssertionFailedError(
			        "Classes were not assignment-compatible : source<" + source.getName() +
			        "> target<" + target.getName() + ">"
			);
		}
	}


	// test skipping ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected static final Log SKIP_LOG = LogFactory.getLog("org.hibernate.test.SKIPPED");

	public String fullTestName() {
		return this.getName() + " (" + this.getClass().getName() + ")";
	}

	protected void reportSkip(String reason, String testDescription) {
		SKIP_LOG.warn( "*** skipping [" + fullTestName() + "] - " + testDescription + " : " + reason, new Exception()  );
	}

	/**
	 * Intended to indicate that this test class as a whole is intended for
	 * a dialect or series of dialects.  Skips here (appliesTo = false) therefore
	 * simply indicate that the given tests target a particular feature of the
	 * checked database and none of the tests on this class should be run for the
	 * checked dialect.
	 *
	 * @param dialect The dialect to be checked.
	 * @return True if all the tests on this class apply to the given dialect (and
	 * therefore should be run); false otherwise.
	 */
	public boolean appliesTo(Dialect dialect) {
		return true;
	}

	/**
	 * Is connection at least read committed?
	 * <p/>
	 * Not, that this skip check relies on the JDBC driver reporting
	 * the true isolation level correctly.  HSQLDB, for example, will
	 * report whatever you specify as the isolation
	 * (Connection.setTransationIsolation()), even though it only supports
	 * read-uncommitted.
	 *
	 * @param scenario text description of the scenario being tested.
	 * @return true if read-committed isolation is maintained.
	 */
	protected boolean readCommittedIsolationMaintained(String scenario) {
		int isolation = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
		Session testSession = null;
		try {
			testSession = openSession();
			isolation = testSession.connection().getTransactionIsolation();
		}
		catch( Throwable ignore ) {
		}
		finally {
			if ( testSession != null ) {
				try {
					testSession.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}
		if ( isolation < java.sql.Connection.TRANSACTION_READ_COMMITTED ) {
			reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Does the db/dialect support using a column's physical name in the order-by clause
	 * even after it has been aliased in the select clause.  This is not actually
	 * required by the SQL spec, although virtually ever DB in the world supports this
	 * (the most glaring omission here being IBM-variant DBs ala DB2 and Derby).
	 *
	 * @param testDescription description of the scenario being tested.
	 * @return true if is allowed
	 */
	protected boolean allowsPhysicalColumnNameInOrderby(String testDescription) {
		if ( DB2Dialect.class.isInstance( getDialect() ) ) {
			// https://issues.apache.org/jira/browse/DERBY-1624
			reportSkip( "Dialect does not support physical column name in order-by clause after it is aliased", testDescription );
			return false;
		}
		return true;
	}

	/**
	 * Does the db/dialect support using a column's physical name in the having clause
	 * even after it has been aliased in the select/group-by clause.  This is not actually
	 * required by the SQL spec, although virtually ever DB in the world supports this.
	 *
	 * @param testDescription description of the scenario being tested.
	 * @return true if is allowed
	 */
	protected boolean allowsPhysicalColumnNameInHaving(String testDescription) {
		// I only *know* of this being a limitation on Derby, although I highly suspect
		// it is a limitation on any IBM/DB2 variant
		if ( DerbyDialect.class.isInstance( getDialect() ) ) {
			// https://issues.apache.org/jira/browse/DERBY-1624
			reportSkip( "Dialect does not support physical column name in having clause after it is aliased", testDescription );
			return false;
		}
		return true;
	}

	/**
	 * Does the db/dialect support empty lists in the IN operator?
	 * <p/>
	 * For example, is "... a.b IN () ..." supported?
	 *
	 * @param testDescription description of the scenario being tested.
	 * @return true if is allowed
	 */
	protected boolean dialectSupportsEmptyInList(String testDescription) {
		if ( ! getDialect().supportsEmptyInList() ) {
			reportSkip( "Dialect does not support SQL empty in list : x in ()", testDescription );
			return false;
		}
		return true;
	}

	/**
	 * Is the db/dialect sensitive in terms of string comparisons?
	 * @param testDescription description of the scenario being tested.
	 * @return true if sensitive
	 */
	protected boolean dialectIsCaseSensitive(String testDescription) {
		if ( getDialect().areStringComparisonsCaseInsensitive() ) {
			reportSkip( "Dialect is case sensitive. ", testDescription );
			return true;
		}
		return false;
	}

	protected boolean supportsRowValueConstructorSyntaxInInList() {
		if ( ! getDialect().supportsRowValueConstructorSyntaxInInList() ) {
			reportSkip( "Dialect does not support 'tuple' syntax as part of an IN value list", "query support" );
			return false;
		}
		return true;
	}

	protected boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		if ( ! getDialect().supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() ) {
			reportSkip( "Driver does not support 'position query' methods on forward-only cursors", "query support" );
			return false;
		}
		return true;
	}

	protected boolean supportsCircularCascadeDelete() {
		if ( ! getDialect().supportsCircularCascadeDeleteConstraints() ) {
			reportSkip( "db/dialect does not support 'circular' cascade delete constraints", "cascade delete constraint support" );
			return false;
		}
		return true;
	}
	
	protected boolean supportsSubselectOnLeftSideIn() {
		if ( ! getDialect().supportsSubselectAsInPredicateLHS() ) {
			reportSkip( "Database does not support (<subselect>) in ( ... ) ", "query support" );
			return false;
		}
		return true;
	}

	/**
	 * Expected LOB usage pattern is such that I can perform an insert
	 * via prepared statement with a parameter binding for a LOB value
	 * without crazy casting to JDBC driver implementation-specific classes...
	 * <p/>
	 * Part of the trickiness here is the fact that this is largely
	 * driver dependent.  For Oracle, which is notoriously bad with
	 * LOB support in their drivers actually does a pretty good job with
	 * LOB support as of the 10.2.x versions of their drivers...
	 *
	 * @return True if expected usage pattern is support; false otherwise.
	 */
	protected boolean supportsExpectedLobUsagePattern() {
		if ( ! getDialect().supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
	}

	/**
	 * Does the current dialect support propogating changes to LOB
	 * values back to the database?  Talking about mutating the
	 * underlying value as opposed to supplying a new
	 * LOB instance...
	 *
	 * @return True if the changes are propogated back to the
	 * database; false otherwise.
	 */
	protected boolean supportsLobValueChangePropogation() {
		if ( ! getDialect().supportsLobValueChangePropogation() ) {
			reportSkip( "database/driver does not support propogating LOB value change back to database", "LOB support" );
			return false;
		}
		return true;
	}

	/**
	 * Is it supported to materialize a LOB locator outside the transaction in
	 * which it was created?
	 * <p/>
	 * Again, part of the trickiness here is the fact that this is largely
	 * driver dependent.
	 * <p/>
	 * NOTE: all database I have tested which {@link #supportsExpectedLobUsagePattern()}
	 * also support the ability to materialize a LOB outside the owning transaction...
	 *
	 * @return True if unbounded materialization is supported; false otherwise.
	 */
	protected boolean supportsUnboundedLobLocatorMaterialization() {
		if ( !getDialect().supportsUnboundedLobLocatorMaterialization() ) {
			reportSkip( "database/driver does not support materializing a LOB locator outside the 'owning' transaction", "LOB support" );
			return false;
		}
		return true;
	}

	protected boolean supportsSubqueryOnMutatingTable() {
		if ( !getDialect().supportsSubqueryOnMutatingTable() ) {
			reportSkip( "database/driver does not support referencing mutating table in subquery", "bulk DML support" );
			return false;
		}
		return true;
	}

	protected boolean dialectIs(Class dialectClass) {
		return dialectClass.isInstance( getDialect() );
	}

	protected boolean dialectIsOneOf(Class[] dialectClasses) {
		for ( int i = 0; i < dialectClasses.length; i++ ) {
			if ( dialectClasses[i].isInstance( getDialect() ) ) {
				return true;
			}
		}
		return false;
	}

	protected boolean dialectIsNot(Class dialectClass) {
		return ! dialectIs( dialectClass );
	}

	protected boolean dialectIsNot(Class[] dialectClasses) {
		return ! dialectIsOneOf( dialectClasses );
	}

}