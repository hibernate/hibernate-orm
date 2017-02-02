/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: A320.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.onetoone.primarykey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.junit.Assert;
import org.junit.Test;

import org.jboss.logging.Logger;

/**
 * Test harness for ANN-742.
 *
 * @author Hardy Ferentschik
 *
 */
public class NullablePrimaryKeyTest {
	private static final Logger log = Logger.getLogger( NullablePrimaryKeyTest.class );

	@Test
	@SuppressWarnings("unchecked")
	public void testGeneratedSql() {

		Map settings = new HashMap();
		settings.putAll( Environment.getProperties() );
		settings.put( AvailableSettings.DIALECT, SQLServerDialect.class.getName() );

		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( settings );

		try {
			MetadataSources ms = new MetadataSources( serviceRegistry );
			ms.addAnnotatedClass(Address.class);
			ms.addAnnotatedClass(Person.class);

			final Metadata metadata = ms.buildMetadata();
			final List<String> commands = new SchemaCreatorImpl( serviceRegistry ).generateCreationCommands( metadata, false );
			for (String s : commands) {
                log.debug( s );
			}
			String expectedMappingTableSql = "create table personAddress (address_id numeric(19,0), " +
					"person_id numeric(19,0) not null, primary key (person_id))";

            Assert.assertEquals( "Wrong SQL", expectedMappingTableSql, commands.get( 2 ) );
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
