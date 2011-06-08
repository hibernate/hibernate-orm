/*
 * Created on Jun 7, 2011
 */
package org.hibernate.jdbc;

import static org.junit.Assert.*;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.Test;

/**
 * @author Dmitry Geraskov (geraskov@gmail.com)
 *
 */
public class JdbcServicesTest {
	
	@Test
	public void testJdbcServices() {
		Configuration cfg = new Configuration();
		cfg.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "true");
		cfg.setProperty("hibernate.connection.driver_class", "some.not.existing.Driver");
		//cfg.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		ServiceRegistryBuilder builder = new ServiceRegistryBuilder();
		builder.applySettings(cfg.getProperties());
		ServiceRegistry serviceRegistry = builder.buildServiceRegistry();
		
		try{
			boolean expToDB = false;
			SchemaExport export = new SchemaExport(serviceRegistry, cfg);
			export.execute(true, expToDB, false, true);
			Dialect dialect = serviceRegistry.getService( JdbcServices.class ).getDialect();
			assertNotNull(dialect != null);
		} catch (Exception e){
			fail("JdbcServices should not fail if jdbc driver not found");
		}
		
	}

}
