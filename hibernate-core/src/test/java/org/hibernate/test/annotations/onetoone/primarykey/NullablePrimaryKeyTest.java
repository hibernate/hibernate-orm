//$Id: A320.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.onetoone.primarykey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * Test harness for ANN-742.
 *
 * @author Hardy Ferentschik
 *
 */
@FailureExpectedWithNewMetamodel( message = "requires support for @OneToOne with mappedBy" )
public class NullablePrimaryKeyTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( NullablePrimaryKeyTest.class );
    @Test
	public void testGeneratedSql() {
		Properties properties = new Properties();
		properties.putAll( Environment.getProperties() );
		properties.setProperty( AvailableSettings.DIALECT, SQLServerDialect.class.getName() );
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( properties );
		try {
			MetadataSources metadataSource = new MetadataSources(serviceRegistry);
			metadataSource.addAnnotatedClass( Address.class ).addAnnotatedClass( Person.class );
			MetadataImplementor metadata = (MetadataImplementor) metadataSource.buildMetadata();
			metadata.getDatabase().getJdbcEnvironment();

			SchemaManagementTool schemaManagementTool = serviceRegistry.getService( SchemaManagementTool.class );
			SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator( new HashMap() );
			final List<String> commands = new ArrayList<String>();
			final org.hibernate.tool.schema.spi.Target target = new org.hibernate.tool.schema.spi.Target() {
				@Override
				public boolean acceptsImportScriptActions() {
					return false;
				}

				@Override
				public void prepare() {
					commands.clear();
				}

				@Override
				public void accept(String command) {
					commands.add( command );
				}

				@Override
				public void release() {
				}
			};
			schemaCreator.doCreation( metadata.getDatabase(), false, target );
			for ( String s : commands ) {
				log.debug( s );
			}
			String expectedMappingTableSql = "create table personAddress (person_id numeric(19,0) not null, " +
					"address_id numeric(19,0), primary key (person_id))";
            Assert.assertEquals( "Wrong SQL", expectedMappingTableSql, commands.get( 2 ) );
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}
