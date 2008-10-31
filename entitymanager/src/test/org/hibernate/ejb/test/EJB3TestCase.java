//$Id$
package org.hibernate.ejb.test;

import java.sql.Blob;
import java.sql.Clob;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.event.EJB3AutoFlushEventListener;
import org.hibernate.ejb.event.EJB3FlushEventListener;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Gavin King
 */
public abstract class EJB3TestCase extends junit.framework.TestCase {
	private static SessionFactory sessions;
	private static Configuration cfg;
	private static Dialect dialect;
	private static Class lastTestClass;
	private org.hibernate.classic.Session session;

	protected boolean recreateSchema() {
		return true;
	}

	public EJB3TestCase(String x) {
		super( x );
	}

	private void buildSessionFactory(String[] files) throws Exception {

		if ( getSessions() != null ) getSessions().close();

		try {

			setCfg( new Configuration() );

			cfg.addProperties( getExtraProperties() );

			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}

			for ( int i = 0; i < files.length ; i++ ) {
				if ( !files[i].startsWith( "net/" ) ) files[i] = getBaseForMappings() + files[i];
				getCfg().addResource( files[i], TestCase.class.getClassLoader() );
			}

			setDialect( Dialect.getDialect() );

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
							if ( "blob".equals( type ) || "clob".equals( type ) ) hasLob = true;
							if ( Blob.class.getName().equals( type ) || Clob.class.getName().equals( type ) ) {
								hasLob = true;
							}
						}
					}
					if ( !hasLob && !clazz.isInherited() ) {
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

			setSessions( getCfg().buildSessionFactory( /*new TestInterceptor()*/ ) );

		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	public String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	protected void setUp() throws Exception {
		if ( getSessions() == null || lastTestClass != getClass() ) {
			buildSessionFactory( getMappings() );
			lastTestClass = getClass();
		}
	}

	protected void runTest() throws Throwable {
		final boolean stats = ( (SessionFactoryImplementor) sessions ).getStatistics().isStatisticsEnabled();
		try {
			if ( stats ) sessions.getStatistics().clear();

			super.runTest();

			if ( stats ) sessions.getStatistics().logSummary();

			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) session.connection().rollback();
				session.close();
				session = null;
				fail( "unclosed session" );
			}
			else {
				session = null;
			}
		}
		catch (Throwable e) {
			try {
				if ( session != null && session.isOpen() ) {
					if ( session.isConnected() ) session.connection().rollback();
					session.close();
				}
			}
			catch (Exception ignore) {
			}
			try {
				if ( dropAfterFailure() && sessions != null ) {
					sessions.close();
					sessions = null;
				}
			}
			catch (Exception ignore) {
			}
			throw e;
		}
	}

	protected boolean dropAfterFailure() {
		return true;
	}

	public org.hibernate.classic.Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	public org.hibernate.classic.Session openSession(Interceptor interceptor)
			throws HibernateException {
		session = getSessions().openSession( interceptor );
		return session;
	}

	protected abstract String[] getMappings();

	private void setSessions(SessionFactory sessions) {
		EJB3TestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	private void setDialect(Dialect dialect) {
		EJB3TestCase.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	protected static void setCfg(Configuration cfg) {
		EJB3TestCase.cfg = cfg;
	}

	protected static Configuration getCfg() {
		return cfg;
	}

	/**
	 * @deprecated
	 */
	public Properties getExtraProperties() {
		return new Properties();
	}

	protected String getBaseForMappings() {
		return "org/hibernate/ejb/test/";
	}

	protected void configure(Configuration cfg) {
		cfg.setListener( "flush", EJB3FlushEventListener.INSTANCE );
		cfg.setListeners( "auto-flush", new AutoFlushEventListener[]{EJB3AutoFlushEventListener.INSTANCE} );
	}

}
