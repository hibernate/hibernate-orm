/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import org.hibernate.MappingException;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentErrorTest {

	@Test
	public void testError() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Book.class )
					.getMetadataBuilder()
					.build();
			Assertions.fail( "Expected a failure" );
		}
		catch (MappingException ex) {
			Assertions.assertTrue( ex.getMessage().contains( Publisher1.class.getName() ) );
			Assertions.assertTrue( ex.getMessage().contains( Publisher2.class.getName() ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		@Struct( name = "publisher_type")
		private Publisher1 ebookPublisher;
		@Struct( name = "publisher_type")
		private Publisher2 paperBackPublisher;
	}

	@Embeddable
	public static class Publisher1 {
		private String name1;
	}
	@Embeddable
	public static class Publisher2 {
		private String name2;
	}

}
