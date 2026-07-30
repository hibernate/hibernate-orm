/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BaseUnitTest
@JiraKey("HHH-20611")
@Exclude
class QuotedEntityNameTest {

	@Test
	void backtickQuotedEntityNameIsRejected() {
		assertQuotedEntityNameRejected( BacktickQuotedEntityName.class );
	}

	@Test
	void doubleQuotedEntityNameIsRejected() {
		assertQuotedEntityNameRejected( DoubleQuotedEntityName.class );
	}

	private static void assertQuotedEntityNameRejected(Class<?> entityClass) {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final AnnotationException exception = assertThrows(
					AnnotationException.class,
					() -> new MetadataSources( serviceRegistry )
							.addAnnotatedClass( entityClass )
							.buildMetadata()
			);
			assertTrue( exception.getMessage().contains( "is quoted" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Entity(name = "`BacktickQuotedEntityName`")
	static class BacktickQuotedEntityName {
		@Id
		private Long id;
	}

	@Entity(name = "\"DoubleQuotedEntityName\"")
	static class DoubleQuotedEntityName {
		@Id
		private Long id;
	}
}
