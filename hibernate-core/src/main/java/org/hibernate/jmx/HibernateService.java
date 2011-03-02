//$Id: HibernateService.java 6100 2005-03-17 10:48:03Z turin42 $
package org.hibernate.jmx;

import java.util.Properties;
import java.util.Map;

import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.util.ExternalSessionFactoryConfig;


/**
 * Implementation of <tt>HibernateServiceMBean</tt>. Creates a
 * <tt>SessionFactory</tt> and binds it to the specified JNDI name.<br>
 * <br>
 * All mapping documents are loaded as resources by the MBean.
 * @see HibernateServiceMBean
 * @see org.hibernate.SessionFactory
 * @author John Urberg, Gavin King
 */
public class HibernateService extends ExternalSessionFactoryConfig implements HibernateServiceMBean {
	private static final Logger log = LoggerFactory.getLogger( HibernateServiceMBean.class );

	private String boundName;
	private Properties properties = new Properties();

	@Override
	public void start() throws HibernateException {
		boundName = getJndiName();
		try {
			buildSessionFactory();
		}
		catch (HibernateException he) {
			log.info( "Could not build SessionFactory using the MBean classpath - will try again using client classpath: " + he.getMessage() );
			log.debug("Error was", he);
			new SessionFactoryStub(this);
		}
	}

	@Override
	public void stop() {
		log.info("stopping service");
		try {
			InitialContext context = JndiHelper.getInitialContext( buildProperties() );
			( (SessionFactory) context.lookup(boundName) ).close();
			//context.unbind(boundName);
		}
		catch (Exception e) {
			log.warn("exception while stopping service", e);
		}
	}
	
	SessionFactory buildSessionFactory() throws HibernateException {
		log.info( "starting service at JNDI name: " + boundName );
		log.info( "service properties: " + properties );
		return buildConfiguration().buildSessionFactory( new ServiceRegistryImpl( properties ) );
	}

	@Override
	protected Map getExtraProperties() {
		return properties;
	}

	@Override
	public String getTransactionStrategy() {
		return getProperty(Environment.TRANSACTION_STRATEGY);
	}

	@Override
	public void setTransactionStrategy(String txnStrategy) {
		setProperty(Environment.TRANSACTION_STRATEGY, txnStrategy);
	}

	@Override
	public String getUserTransactionName() {
		return getProperty(Environment.USER_TRANSACTION);
	}

	@Override
	public void setUserTransactionName(String utName) {
		setProperty(Environment.USER_TRANSACTION, utName);
	}

	@Override
	public String getJtaPlatformName() {
		return getProperty( JtaPlatformInitiator.JTA_PLATFORM );
	}

	@Override
	public void setJtaPlatformName(String name) {
		setProperty( JtaPlatformInitiator.JTA_PLATFORM, name );
	}

	@Override
	public String getPropertyList() {
		return buildProperties().toString();
	}

	@Override
	public String getProperty(String property) {
		return properties.getProperty(property);
	}

	@Override
	public void setProperty(String property, String value) {
		properties.setProperty(property, value);
	}

	@Override
	public void dropSchema() {
		new SchemaExport( buildConfiguration() ).drop(false, true);
	}

	@Override
	public void createSchema() {
		new SchemaExport( buildConfiguration() ).create(false, true);
	}

	public String getName() {
		return getProperty(Environment.SESSION_FACTORY_NAME);
	}

	@Override
	public String getDatasource() {
		return getProperty(Environment.DATASOURCE);
	}

	@Override
	public void setDatasource(String datasource) {
		setProperty(Environment.DATASOURCE, datasource);
	}

	@Override
	public String getJndiName() {
		return getProperty(Environment.SESSION_FACTORY_NAME);
	}

	@Override
	public void setJndiName(String jndiName) {
		setProperty(Environment.SESSION_FACTORY_NAME, jndiName);
	}

	@Override
	public String getUserName() {
		return getProperty(Environment.USER);
	}

	@Override
	public void setUserName(String userName) {
		setProperty(Environment.USER, userName);
	}

	@Override
	public String getPassword() {
		return getProperty(Environment.PASS);
	}

	@Override
	public void setPassword(String password) {
		setProperty(Environment.PASS, password);
	}

	@Override
	public void setFlushBeforeCompletionEnabled(String enabled) {
		setProperty(Environment.FLUSH_BEFORE_COMPLETION, enabled);
	}

	@Override
	public String getFlushBeforeCompletionEnabled() {
		return getProperty(Environment.FLUSH_BEFORE_COMPLETION);
	}

	@Override
	public void setAutoCloseSessionEnabled(String enabled) {
		setProperty(Environment.AUTO_CLOSE_SESSION, enabled);
	}

	@Override
	public String getAutoCloseSessionEnabled() {
		return getProperty(Environment.AUTO_CLOSE_SESSION);
	}

	public Properties getProperties() {
		return buildProperties();
	}
}
