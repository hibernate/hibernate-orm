/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.junit.functional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.sql.Connection;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.SessionFactory;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.engine.SessionFactoryImplementor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Most of the Hibernate test suite in fact is a series of functional tests, not
 * unit tests.  Here is a base class for these functional tests.
 *
 * @author Steve Ebersole
 */
public abstract class FunctionalTestCase extends UnitTestCase implements ExecutionEnvironment.Settings {

	private static final Logger log = LoggerFactory.getLogger( FunctionalTestCase.class );

	private ExecutionEnvironment environment;
	private boolean isEnvironmentLocallyManaged;

	private org.hibernate.classic.Session session;

	public FunctionalTestCase(String string) {
		super( string );
	}

	public ExecutionEnvironment getEnvironment() {
		return environment;
	}

	public void setEnvironment(ExecutionEnvironment environment) {
		this.environment = environment;
	}

	protected void prepareTest() throws Exception {
	}

	protected void cleanupTest() throws Exception {
	}

	// JUnit hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Override {@link junit.framework.TestCase#setUp()} to check if we need
	 * to build a locally managed execution environment.
	 *
	 * @throws Exception
	 */
	protected final void setUp() throws Exception {
		if ( environment == null ) {
			log.info( "Building locally managed execution env" );
			isEnvironmentLocallyManaged = true;
			environment = new ExecutionEnvironment( this );
			environment.initialize();
		}
		prepareTest();
	}

	/**
	 * Override {@link junit.framework.TestCase#tearDown()} to tear down
	 * the execution environment if it is locally managed.
	 *
	 * @throws Exception
	 */
	protected final void tearDown() throws Exception {
		cleanupTest();
		if ( isEnvironmentLocallyManaged ) {
			log.info( "Destroying locally managed execution env" );
			environment.complete();
			environment = null;
		}
	}

	/**
	 * runTest is overridden in order to apply session closure assertions.
	 *
	 * @throws Throwable
	 */
	protected void runTest() throws Throwable {
		final boolean stats = sfi().getStatistics().isStatisticsEnabled();
		try {
			if ( stats ) {
				sfi().getStatistics().clear();
			}

			super.runTest();

			if ( stats ) {
				sfi().getStatistics().logSummary();
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
			log.trace( "test run resulted in error; attempting to cleanup", e );
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
				if ( recreateSchemaAfterFailure() && environment != null ) {
					environment.rebuild();
				}
			}
			catch ( Exception ignore ) {
			}
			throw e;
		}
	}

	protected void assertAllDataRemoved() {
		if ( !createSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( "hibernate.test.validateDataCleanup" ) ) {
			return;
		}

		Session tmpSession = getSessions().openSession();
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

	protected void skipExpectedFailure(Throwable error) {
		super.skipExpectedFailure( error );
		try {
			if ( recreateSchemaAfterFailure() && environment != null ) {
				environment.rebuild();
			}
		}
		catch ( Exception ignore ) {
		}
	}

	// ExecutionEnvironment.Settings implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	public boolean createSchema() {
		return true;
	}

	public boolean recreateSchemaAfterFailure() {
		return true;
	}

	public void configure(Configuration cfg) {
	}

	public boolean overrideCacheStrategy() {
		return true;
	}

	public String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	public void afterSessionFactoryBuilt(SessionFactoryImplementor sfi) {
	}

	public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
	}

	/**
	 * Intended to indicate that this test class as a whole is intended for
	 * a dialect or series of dialects.  Skips here (appliesTo = false) therefore
	 * simply indicate that the given tests target a particular feature of the
	 * checked database and none of the tests on this class should be run for the
	 * checked dialect.
	 *
	 * @param dialect The dialect to be checked.
	 * @return False if the test class as a whole is specifically targetting
	 * a dialect (or series of dialects) other than the indicated dialect
	 * and the test should therefore be skipped in its entirety;
	 * true otherwise.
	 */
	public boolean appliesTo(Dialect dialect) {
		return true;
	}


	// methods for subclasses to access environment ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the factory for this test environment.
	 *
	 * @return The factory.
	 */
	protected SessionFactory getSessions() {
		return environment.getSessionFactory();
	}

	/**
	 * Get the factory for this test environment, casted to {@link org.hibernate.engine.SessionFactoryImplementor}.
	 * <p/>
	 * Shorthand for ( {@link org.hibernate.engine.SessionFactoryImplementor} ) {@link #getSessions()}...
	 *
	 * @return The factory
	 */
	protected SessionFactoryImplementor sfi() {
		return ( SessionFactoryImplementor ) getSessions();
	}

	protected Dialect getDialect() {
		return ExecutionEnvironment.DIALECT;
	}

	protected Configuration getCfg() {
		return environment.getConfiguration();
	}

	public org.hibernate.classic.Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public org.hibernate.classic.Session openSession(Interceptor interceptor) throws HibernateException {
		session = getSessions().openSession(interceptor);
		return session;
	}



	/**
	 * Do connections enforce SERIALIZABLE isolation...
	 *
	 * @return
	 * @throws Exception
	 */
	protected boolean isSerializableIsolationEnforced() throws Exception {
		Connection conn = null;
		try {
			conn = sfi().getConnectionProvider().getConnection();
			return conn.getTransactionIsolation() >= Connection.TRANSACTION_SERIALIZABLE;
		}
		finally {
			if ( conn != null ) {
				try {
					sfi().getConnectionProvider().closeConnection( conn );
				}
				catch ( Throwable ignore ) {
					// ignore...
				}
			}
		}
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
		if ( ! getDialect().areStringComparisonsCaseInsensitive() ) {
			reportSkip( "Dialect is case sensitive. ", testDescription );
			return true;
		}
		return false;
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
