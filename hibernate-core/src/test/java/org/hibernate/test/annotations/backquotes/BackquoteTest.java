//$Id$
package org.hibernate.test.annotations.backquotes;
import static org.hibernate.TestLogger.LOG;
import java.io.PrintWriter;
import java.io.StringWriter;
import junit.framework.TestCase;
import org.hibernate.MappingException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * Testcase for ANN-718 - @JoinTable / @JoinColumn fail when using backquotes in PK field name.
 *
 * @author Hardy Ferentschik
 *
 */
public class BackquoteTest extends TestCase {

    private ServiceRegistryHolder serviceRegistryHolder;

	@Override
    protected void setUp() {
		serviceRegistryHolder = new ServiceRegistryHolder( Environment.getProperties() );
	}

	@Override
    protected void tearDown() {
		if ( serviceRegistryHolder != null ) {
			serviceRegistryHolder.destroy();
		}
	}

	public void testBackquotes() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Bug.class);
			config.addAnnotatedClass(Category.class);
			config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            LOG.debug(writer.toString());
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
	public void testInvalidReferenceToQuotedTableName() {
    	try {
    		AnnotationConfiguration config = new AnnotationConfiguration();
    		config.addAnnotatedClass(Printer.class);
    		config.addAnnotatedClass(PrinterCable.class);
    		config.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
    		fail("expected MappingException to be thrown");
    	}
    	//we WANT MappingException to be thrown
        catch( MappingException e ) {
        	assertTrue("MappingException was thrown", true);
        }
        catch(Exception e) {
        	StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            LOG.debug(writer.toString());
        	fail(e.getMessage());
        }
	}
}
