/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.naturalid.composite;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-11255")
public class CompositeNaturalIdMappingTest extends BaseUnitTestCase {

	@Test
	public void test() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.build();

		try {
			Metadata meta = new MetadataSources( ssr )
					.addAnnotatedClass( PostalCarrier.class )
					.addAnnotatedClass( PostalCode.class )
					.buildMetadata();
			( (MetadataImplementor) meta ).validate();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
