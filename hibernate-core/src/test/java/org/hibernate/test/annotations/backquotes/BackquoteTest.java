//$Id$
package org.hibernate.test.annotations.backquotes;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testcase for ANN-718 - @JoinTable / @JoinColumn fail when using backquotes in PK field name.
 *
 * @author Hardy Ferentschik
 *
 */
public class BackquoteTest extends BaseCoreFunctionalTestMethod {
	private static final Logger log = Logger.getLogger( BackquoteTest.class );


	@Test
	@TestForIssue( jiraKey = "ANN-718" )
	public void testBackquotes() {
		try {
			getTestConfiguration().addAnnotatedClass( Bug.class ).addAnnotatedClass( Category.class );
			getSessionFactoryHelper().getSessionFactory();
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}

	/**
	 *  HHH-4647 : Problems with @JoinColumn referencedColumnName and quoted column and table names
	 *
	 *  An invalid referencedColumnName to an entity having a quoted table name results in an
	 *  infinite loop in o.h.c.Configuration$MappingsImpl#getPhysicalColumnName().
	 *  The same issue exists with getLogicalColumnName()
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-4647" )
	public void testInvalidReferenceToQuotedTableName() {
    	try {
			getTestConfiguration().addAnnotatedClass( Printer.class ).addAnnotatedClass( PrinterCable.class );
			getSessionFactoryHelper().getSessionFactory();
    		fail("expected MappingException to be thrown");
    	}
    	//we WANT MappingException to be thrown
        catch( MappingException e ) {
        	assertTrue("MappingException was thrown", true);
        }
        catch(Exception e) {
        	StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
        	fail(e.getMessage());
        }
	}
}
