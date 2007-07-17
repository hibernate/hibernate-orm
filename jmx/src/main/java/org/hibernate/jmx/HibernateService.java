//$Id: HibernateService.java 6100 2005-03-17 10:48:03Z turin42 $
package org.hibernate.jmx;

import java.util.Properties;
import java.util.Map;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.util.NamingHelper;
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

	private static final Log log = LogFactory.getLog(HibernateServiceMBean.class);

	private String boundName;
	private Properties properties = new Properties();


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

	public void stop() {
		log.info("stopping service");
		try {
			InitialContext context = NamingHelper.getInitialContext( buildProperties() );
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
		return buildConfiguration().buildSessionFactory();
	}

	protected Map getExtraProperties() {
		return properties;
	}

	public String getTransactionStrategy() {
		return getProperty(Environment.TRANSACTION_STRATEGY);
	}

	public void setTransactionStrategy(String txnStrategy) {
		setProperty(Environment.TRANSACTION_STRATEGY, txnStrategy);
	}

	public String getUserTransactionName() {
		return getProperty(Environment.USER_TRANSACTION);
	}

	public void setUserTransactionName(String utName) {
		setProperty(Environment.USER_TRANSACTION, utName);
	}

	public String getTransactionManagerLookupStrategy() {
		return getProperty(Environment.TRANSACTION_MANAGER_STRATEGY);
	}

	public void setTransactionManagerLookupStrategy(String lkpStrategy) {
		setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, lkpStrategy);
	}

	public String getPropertyList() {
		return buildProperties().toString();
	}

	public String getProperty(String property) {
		return properties.getProperty(property);
	}

	public void setProperty(String property, String value) {
		properties.setProperty(property, value);
	}

	public void dropSchema() {
		new SchemaExport( buildConfiguration() ).drop(false, true);
	}

	public void createSchema() {
		new SchemaExport( buildConfiguration() ).create(false, true);
	}	public String getName() {
		return getProperty(Environment.SESSION_FACTORY_NAME);
	}

	public String getDatasource() {
		return getProperty(Environment.DATASOURCE);
	}

	public void setDatasource(String datasource) {
		setProperty(Environment.DATASOURCE, datasource);
	}

	public String getJndiName() {
		return getProperty(Environment.SESSION_FACTORY_NAME);
	}

	public void setJndiName(String jndiName) {
		setProperty(Environment.SESSION_FACTORY_NAME, jndiName);
	}

	public String getUserName() {
		return getProperty(Environment.USER);
	}

	public void setUserName(String userName) {
		setProperty(Environment.USER, userName);
	}

	public String getPassword() {
		return getProperty(Environment.PASS);
	}

	public void setPassword(String password) {
		setProperty(Environment.PASS, password);
	}

	public void setFlushBeforeCompletionEnabled(String enabled) {
		setProperty(Environment.FLUSH_BEFORE_COMPLETION, enabled);
	}

	public String getFlushBeforeCompletionEnabled() {
		return getProperty(Environment.FLUSH_BEFORE_COMPLETION);
	}

	public void setAutoCloseSessionEnabled(String enabled) {
		setProperty(Environment.AUTO_CLOSE_SESSION, enabled);
	}

	public String getAutoCloseSessionEnabled() {
		return getProperty(Environment.AUTO_CLOSE_SESSION);
	}

	public Properties getProperties() {
		return buildProperties();
	}
}
