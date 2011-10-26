//$Id: A320.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.onetoone.primarykey;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * Test harness for ANN-742.
 *
 * @author Hardy Ferentschik
 *
 */
public class NullablePrimaryKeyTest {
	private static final Logger log = Logger.getLogger( NullablePrimaryKeyTest.class );
    @Test
	public void testGeneratedSql() {

		ServiceRegistry serviceRegistry = null;
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
			config.buildSessionFactory( serviceRegistry );
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
			for (String s : schema) {
                log.debug(s);
			}
			String expectedMappingTableSql = "create table personAddress (address_id numeric(19,0), " +
					"person_id numeric(19,0) not null, primary key (person_id))";
            Assert.assertEquals( "Wrong SQL", expectedMappingTableSql, schema[2] );
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}
