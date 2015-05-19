/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping.usertypes;


import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for read-order independent resolution of user-defined types
 * Testcase for issue HHH-7300
 * @author Stefan Schulze
 */
@TestForIssue(jiraKey = "HHH-7300")
public class UserTypeMappingTest extends BaseUnitTestCase{

private Configuration cfg;
private ServiceRegistry serviceRegistry;

	@Before
	public void setup(){
		cfg=new Configuration();
		Properties p = new Properties();
		p.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		p.put( "hibernate.connection.driver_class", "org.h2.Driver" );
		p.put( "hibernate.connection.url", "jdbc:h2:mem:" );
		p.put( "hibernate.connection.username", "sa" );
		p.put( "hibernate.connection.password", "" );
		cfg.setProperties(p);
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
	}

	public void tearDown(){
		if(serviceRegistry!=null){
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
	
	@Test
	public void testFirstTypeThenEntity(){
		cfg.addResource("org/hibernate/test/mapping/usertypes/TestEnumType.hbm.xml")
		   .addResource("org/hibernate/test/mapping/usertypes/TestEntity.hbm.xml");
		SessionFactory sessions=cfg.buildSessionFactory(serviceRegistry);
		Assert.assertNotNull(sessions);
		sessions.close();
	}
	
	@Test
	public void testFirstEntityThenType(){
		cfg.addResource("org/hibernate/test/mapping/usertypes/TestEntity.hbm.xml")
		   .addResource("org/hibernate/test/mapping/usertypes/TestEnumType.hbm.xml");
		
		SessionFactory sessions=cfg.buildSessionFactory(serviceRegistry);
		Assert.assertNotNull(sessions);
		sessions.close();
	}

}
