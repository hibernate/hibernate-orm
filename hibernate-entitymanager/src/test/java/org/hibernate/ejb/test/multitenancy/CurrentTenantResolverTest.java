package org.hibernate.ejb.test.multitenancy;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.ejb.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.junit.Test;

@TestForIssue(jiraKey="HHH-7312")
public class CurrentTenantResolverTest extends
		BaseEntityManagerFunctionalTestCase {

	private Properties properties;
	private TestCurrentTenantIdentifierResolver tenantResolver;

	@Override
	protected Dialect getDialect() {
		return H2Dialect.getDialect();
	}
	
	@Override
	protected Ejb3Configuration constructConfiguration() {		
		Ejb3Configuration ejb3cfg=super.constructConfiguration();
		Configuration cfg=ejb3cfg.getHibernateConfiguration();
		
		properties = new Properties();
		properties.put(org.hibernate.ejb.AvailableSettings.JDBC_DRIVER,
				"org.h2.Driver");
		properties.put(org.hibernate.ejb.AvailableSettings.JDBC_URL,
				"jdbc:h2:mem:");
		properties.put(AvailableSettings.MULTI_TENANT, "SCHEMA");
		tenantResolver = new TestCurrentTenantIdentifierResolver();
		properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,tenantResolver);
		
		TestMultiTenantConnectionProvider provider=new TestMultiTenantConnectionProvider();
		provider.addProvider("acme",ConnectionProviderBuilder.buildConnectionProvider("acme"));
		provider.addProvider("jboss",ConnectionProviderBuilder.buildConnectionProvider("jboss"));
		
		properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,provider);
		
		cfg.setProperties(properties);
		return ejb3cfg;
	}

	@Test
	public void testCurrentTenantIdentifierResolverIsUsed() {
		tenantResolver.setCurrentTenantIdentifier("acme");
		EntityManager em=createEntityManager(properties);
		String usedTenant=((EntityManagerImpl)em).getSession().getTenantIdentifier();
		Assert.assertEquals("acme", usedTenant);
		em.close();
	}
}
