/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.version;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class GeneratedVersionBindingTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		MetadataSources metadataSources = new MetadataSources()
			.addResource("org/hibernate/test/hbm/version/Mappings.hbm.xml");

		try {
			metadataSources.buildMetadata();
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}
}
