/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.collection;

import org.hibernate.AnnotationException;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractEmbeddableWithManyToManyTest {
	@Test
	public void test() {
		try (BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
			StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr ).build()) {
			MappingSources mappingSources = new MappingSources();
			addResources( mappingSources );
			addAnnotatedClasses( mappingSources );

			MetadataBuildingTestHelper.buildMetadata( ssr, mappingSources );
			fail( "Should throw AnnotationException!" );
		}
		catch (AnnotationException expected) {
			assertTrue( expected.getMessage().contains(
					"belongs to an '@Embeddable' class that is contained in an '@ElementCollection' and may not be"
			) );
		}
	}

	protected void addAnnotatedClasses(MappingSources mappingSources) {

	}

	protected void addResources(MappingSources mappingSources) {

	}
}
