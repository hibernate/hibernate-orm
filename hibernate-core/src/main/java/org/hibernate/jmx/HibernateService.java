//$Id: HibernateService.java 6100 2005-03-17 10:48:03Z turin42 $
package org.hibernate.jmx;

import java.util.Map;
import java.util.Properties;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ExternalSessionFactoryConfig;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;


/**
 * Implementation of <tt>HibernateServiceMBean</tt>. Creates a
 * <tt>SessionFactory</tt> and binds it to the specified JNDI name.<br>
 * <br>
 * All mapping documents are loaded as resources by the MBean.
 * @see HibernateServiceMBean
 * @see org.hibernate.SessionFactory
 * @author John Urberg, Gavin King
 * @deprecated See <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6190">HHH-6190</a> for details
 */
@Deprecated
public class HibernateService extends ExternalSessionFactoryConfig implements HibernateServiceMBean {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, HibernateService.class.getName());

	private String boundName;
	private Properties properties = new Properties();

	@Override
	public void start() throws HibernateException {
		boundName = getJndiName();
		try {
			buildSessionFactory();
		}
		catch (HibernateException he) {
            LOG.unableToBuildSessionFactoryUsingMBeanClasspath(he.getMessage());
            LOG.debug("Error was", he);
			new SessionFactoryStub(this);
		}
	}

	@Override
	public void stop() {
        LOG.stoppingService();
		try {
			InitialContext context = JndiHelper.getInitialContext( buildProperties() );
			( (SessionFactory) context.lookup(boundName) ).close();
			//context.unbind(boundName);
		}
		catch (Exception e) {
            LOG.unableToStopHibernateService(e);
		}
	}

	SessionFactory buildSessionFactory() throws HibernateException {
        LOG.startingServiceAtJndiName( boundName );
        LOG.serviceProperties( properties );
        return buildConfiguration().buildSessionFactory(
				new ServiceRegistryBuilder().applySettings( properties ).buildServiceRegistry()
		);
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
		return getProperty( AvailableSettings.JTA_PLATFORM );
	}

	@Override
	public void setJtaPlatformName(String name) {
		setProperty( AvailableSettings.JTA_PLATFORM, name );
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
