/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance.spread;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7129" )
public class SpreadNaturalIdTest extends BaseUnitTestCase {
	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSpreadNaturalIdDeclarationGivesMappingException() {
		final MetadataSources metadataSources = new MetadataSources()
			.addAnnotatedClass( Principal.class )
			.addAnnotatedClass( User.class );
		try {

			metadataSources.buildMetadata();
			fail( "Expected binders to throw an exception" );
		}
		catch (AnnotationException expected) {
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}
}
