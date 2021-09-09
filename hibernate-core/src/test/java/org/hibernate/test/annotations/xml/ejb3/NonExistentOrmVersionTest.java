/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.internal.util.xml.UnsupportedOrmXsdVersionException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

@TestForIssue(jiraKey = {"HHH-6271", "HHH-14529"})
public class NonExistentOrmVersionTest extends BaseUnitTestCase {
	@Test
	public void testNonExistentOrmVersion() {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			new MetadataSources( serviceRegistry )
					.addResource( "org/hibernate/test/annotations/xml/ejb3/orm5.xml" )
					.buildMetadata();
			fail( "Expecting failure due to unsupported xsd version" );
		}
		catch ( InvalidMappingException expected ) {
		}
		catch ( UnsupportedOrmXsdVersionException expected ) {
		}
	}
}
