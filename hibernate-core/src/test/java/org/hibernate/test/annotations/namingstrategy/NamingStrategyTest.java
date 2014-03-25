// $Id$
package org.hibernate.test.annotations.namingstrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
public class NamingStrategyTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( NamingStrategyTest.class );

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
	public void testWithCustomNamingStrategy() throws Exception {
		try {
			MetadataSources metadataSources = new MetadataSources()
					.addAnnotatedClass( Address.class )
					.addAnnotatedClass( Person.class );
			metadataSources.getMetadataBuilder().with( new DummyNamingStrategy() ).build();
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}

    @Test
	public void testWithEJB3NamingStrategy() throws Exception {
		MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( AddressEntry.class );
		MetadataImplementor metadata = (MetadataImplementor) metadataSources.getMetadataBuilder()
				.with( EJB3NamingStrategy.INSTANCE )
				.build();
		assertNotNull( SchemaUtil.getTable( "A_ADDRESS", metadata ) );
	}

    @Test
	public void testWithoutCustomNamingStrategy() throws Exception {
		try {
			MetadataSources metadataSources = new MetadataSources()
					.addAnnotatedClass( Address.class )
					.addAnnotatedClass( Person.class );
			metadataSources.buildMetadata();
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}
}
