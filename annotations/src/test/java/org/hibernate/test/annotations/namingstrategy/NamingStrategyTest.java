// $Id:$
package org.hibernate.test.annotations.namingstrategy;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.hibernate.cfg.AnnotationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test harness for ANN-716.
 * 
 * @author Hardy Ferentschik
 */
public class NamingStrategyTest extends TestCase {
	
	private Logger log = LoggerFactory.getLogger(NamingStrategyTest.class);

	public void testWithCustomNamingStrategy() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.setNamingStrategy(new DummyNamingStrategy());
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			config.buildSessionFactory();
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			log.debug(writer.toString());
			fail(e.getMessage());
		}
	}
	
	public void testWithoutCustomNamingStrategy() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			config.buildSessionFactory();
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			log.debug(writer.toString());
			fail(e.getMessage());
		}
	}	
}
