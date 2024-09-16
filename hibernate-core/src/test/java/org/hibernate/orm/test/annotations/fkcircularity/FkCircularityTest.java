/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.fkcircularity;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest {

	@Test
	public void testJoinedSublcassesInPK() {
		MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
			.addAnnotatedClass(A.class)
			.addAnnotatedClass(B.class)
			.addAnnotatedClass(C.class)
			.addAnnotatedClass(D.class);
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

	@Test
	public void testDeepJoinedSuclassesHierachy() {
		MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
				.addAnnotatedClass(ClassA.class)
				.addAnnotatedClass(ClassB.class)
				.addAnnotatedClass(ClassC.class)
				.addAnnotatedClass(ClassD.class);
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
