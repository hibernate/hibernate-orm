/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-18288")
public class SubclassIndexTest {

	@Test
	@ServiceRegistry
	void test(ServiceRegistryScope registryScope) {
		try {
			new MetadataSources( registryScope.getRegistry() )
					.addAnnotatedClasses( Foo.class, Bar.class )
					.buildMetadata();
			fail( "Expecting exception" );
		}
		catch (AnnotationException expected) {
			assertThat( expected.getMessage() ).contains( "is a subclass in a 'SINGLE_TABLE' hierarchy and may not be annotated '@Table'" );
		}
	}

	@Entity
	@Table(name = "FOO")
	static class Foo {
		@Id
		long id;
	}

	@Entity
	@Table(indexes = @Index(name="IDX", columnList = "text"))
	static class Bar extends Foo {
		@OneToMany
		@OrderColumn
		List<Foo> foo;

		String text;
	}
}
