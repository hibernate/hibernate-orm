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

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.hibernate.testing.SkipLog;

import org.junit.After;
import org.junit.Before;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}
 *
 * @author Steve Ebersole
 */
public abstract class BaseCoreFunctionalTestCase extends BaseFunctionalTestCase implements TestSessionFactoryHelper.Callback{

	private TestSessionFactoryHelper sessionFactoryBuilder;

	public BaseCoreFunctionalTestCase(){
		initialize();
		getTestConfiguration().setCacheConcurrencyStrategy( getCacheConcurrencyStrategy() );
		getTestConfiguration().setOverrideCacheStrategy( overrideCacheStrategy() );
		sessionFactoryBuilder = new TestSessionFactoryHelper( getTestServiceRegistryHelper(),getTestConfiguration());
		sessionFactoryBuilder.setCallback( this );
	}

	protected Session session;

	protected final SessionFactoryImplementor sessionFactory() {
		return getSessionFactoryBuilder().getSessionFactory();
	}

	protected Session openSession() throws HibernateException {
		session = sessionFactory().openSession();
		return session;
	}

	protected Session openSession(Interceptor interceptor) throws HibernateException {
		session = sessionFactory().withOptions().interceptor( interceptor ).openSession();
		return session;
	}

	public TestSessionFactoryHelper getSessionFactoryBuilder() {
		return sessionFactoryBuilder;
	}

	public void setSessionFactoryBuilder(final TestSessionFactoryHelper sessionFactoryBuilder) {
		this.sessionFactoryBuilder = sessionFactoryBuilder;
	}

	// before/after test class ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@BeforeClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void buildSessionFactory() {
		getSessionFactoryBuilder().buildSessionFactory();
	}

	public void rebuildSessionFactory() {
		releaseSessionFactory();
		buildSessionFactory();
	}

	@Override
	public void afterMetadataBuilt(MetadataImplementor metadataImplementor) {
	}
	@Override
	public void configure(MetadataBuilder metadataBuilder) {
	}
	@Override
	public void configSessionFactoryBuilder(SessionFactoryBuilder sessionFactoryBuilder) {
	}

	@Override
	public void configure(Configuration configuration) {
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	@Override
	public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
	}


	@Override
	public void afterSessionFactoryBuilt() {
	}

	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@AfterClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	private void releaseSessionFactory() {
		if ( getSessionFactoryBuilder() != null ) {
			getSessionFactoryBuilder().destory();
//			setSessionFactoryBuilder( null );
		}
		getTestConfiguration().setConfiguration( null );
		getTestConfiguration().setMetadata( null );
		getTestServiceRegistryHelper().destroy();
	}

	@OnFailure
	@OnExpectedFailure
	@SuppressWarnings( {"UnusedDeclaration"})
	public void onFailure() {
		if ( rebuildSessionFactoryOnError() ) {
			rebuildSessionFactory();
		}
	}

	// before/after each test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Before
	public final void beforeTest() throws Exception {
		prepareTest();
	}

	protected void prepareTest() throws Exception {
	}

	@After
	public final void afterTest() throws Exception {
		if ( isCleanupTestDataRequired() ) {
			cleanupTestData();
		}
		cleanupTest();

		cleanupSession();

		assertAllDataRemoved();

	}

	protected void cleanupCache() {
		if ( sessionFactory() != null ) {
			sessionFactory().getCache().evictCollectionRegions();
			sessionFactory().getCache().evictDefaultQueryRegion();
			sessionFactory().getCache().evictEntityRegions();
			sessionFactory().getCache().evictQueryRegions();
			sessionFactory().getCache().evictNaturalIdRegions();
		}
	}
	
	protected boolean isCleanupTestDataRequired() { return false; }

	protected void cleanupTestData() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from java.lang.Object" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}


	private void cleanupSession() {
		if ( session != null && ! ( (SessionImplementor) session ).isClosed() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
		}
		session = null;
	}

	public class RollbackWork implements Work {
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

	protected void cleanupTest() throws Exception {
	}

	@SuppressWarnings( {"UnnecessaryBoxing", "UnnecessaryUnboxing"})
	protected void assertAllDataRemoved() {
		if ( !createSchema() ) {
			return; // no tables were created...
		}
		if ( !Boolean.getBoolean( VALIDATE_DATA_CLEANUP ) ) {
			return;
		}

		Session tmpSession = sessionFactory().openSession();
		try {
			List list = tmpSession.createQuery( "select o from java.lang.Object o" ).list();

			Map<String,Integer> items = new HashMap<String,Integer>();
			if ( !list.isEmpty() ) {
				for ( Object element : list ) {
					Integer l = items.get( tmpSession.getEntityName( element ) );
					if ( l == null ) {
						l = 0;
					}
					l = l + 1 ;
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

	protected boolean readCommittedIsolationMaintained(String scenario) {
		int isolation = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
		Session testSession = null;
		try {
			testSession = openSession();
			isolation = testSession.doReturningWork(
					new AbstractReturningWork<Integer>() {
						@Override
						public Integer execute(Connection connection) throws SQLException {
							return connection.getTransactionIsolation();
						}
					}
			);
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
			SkipLog.reportSkip( "environment does not support at least read committed isolation", scenario );
			return false;
		}
		else {
			return true;
		}
	}
}
