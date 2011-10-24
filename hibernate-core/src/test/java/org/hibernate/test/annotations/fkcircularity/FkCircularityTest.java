// $Id$
package org.hibernate.test.annotations.fkcircularity;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest {
	private static final Logger log = Logger.getLogger( FkCircularityTest.class );

	private ServiceRegistry serviceRegistry;

	@Before
    public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
    public void tearDown() {
        if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
	}
    @Test
	public void testJoinedSublcassesInPK() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass(A.class);
			config.addAnnotatedClass(B.class);
			config.addAnnotatedClass(C.class);
			config.addAnnotatedClass(D.class);
			config.buildSessionFactory( serviceRegistry );
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
			for (String s : schema) {
                log.debug(s);
			}
            log.debug("success");
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
            Assert.fail( e.getMessage() );
		}
	}
    @Test
	public void testDeepJoinedSuclassesHierachy() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass(ClassA.class);
			config.addAnnotatedClass(ClassB.class);
			config.addAnnotatedClass(ClassC.class);
			config.addAnnotatedClass(ClassD.class);
			config.buildSessionFactory( serviceRegistry );
			String[] schema = config
					.generateSchemaCreationScript(new HSQLDialect());
			for (String s : schema) {
                log.debug(s);
			}
            log.debug("success");
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			Assert.fail(e.getMessage());
		}
	}
}