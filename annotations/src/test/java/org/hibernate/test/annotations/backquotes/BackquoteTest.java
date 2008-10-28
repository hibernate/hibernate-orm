//$Id$
package org.hibernate.test.annotations.backquotes;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.hibernate.cfg.AnnotationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testcase for ANN-718 - @JoinTable / @JoinColumn fail when using backquotes in PK field name.
 * 
 * @author Hardy Ferentschik
 *
 */
public class BackquoteTest extends TestCase {
		
	private Logger log = LoggerFactory.getLogger(BackquoteTest.class);	
	
	public void testBackquotes() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Bug.class);
			config.addAnnotatedClass(Category.class);
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
